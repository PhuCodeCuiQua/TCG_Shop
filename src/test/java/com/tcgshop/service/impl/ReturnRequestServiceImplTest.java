package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.dto.request.CreateReturnRequest;
import com.tcgshop.dto.request.EscalateReturnRequest;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.ReturnRequest;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReturnReason;
import com.tcgshop.entity.enums.ReturnStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.event.ReturnRequestEscalatedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.ReturnRequestRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.OrderService;
import com.tcgshop.service.UploadResult;
import com.tcgshop.util.TransactionUtils;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.InOrder;

class ReturnRequestServiceImplTest {
 private static final Long ORDER_ID=21L, USER_ID=7L;
 private final OrderRepository orders=mock(OrderRepository.class);
 private final ReturnRequestRepository requests=mock(ReturnRequestRepository.class);
 private final CurrentUser current=mock(CurrentUser.class);
 private final OrderService orderService=mock(OrderService.class);
 private final CloudinaryService cloudinary=mock(CloudinaryService.class);
 private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
 private final Clock clock=Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);
 private final ReturnRequestServiceImpl service=new ReturnRequestServiceImpl(orders,requests,current,
   new ReturnWindowPolicy(new OrderProperties(7,3),clock),new OrderProperties(7,3),orderService,cloudinary,events,clock);
 private Order order;
 @BeforeEach void setup(){ order=new Order(); order.setId(ORDER_ID); order.setOrderCode("it-order");
   order.setUser(new User()); order.setStatus(OrderStatus.DELIVERED); order.setDeliveredAt(LocalDateTime.of(2026,2,28,12,0));
   when(current.getId()).thenReturn(USER_ID); when(orders.findByIdAndUserIdForUpdate(ORDER_ID,USER_ID)).thenReturn(Optional.of(order));
   when(requests.existsByOrderId(ORDER_ID)).thenReturn(false);
   when(requests.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
   when(cloudinary.uploadImage(any(),anyString())).thenReturn(new UploadResult("image-url","image-id"));
   when(cloudinary.uploadVideo(any(),anyString())).thenReturn(new UploadResult("video-url","video-id")); }
 @AfterEach void cleanup(){ if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
 private CreateReturnRequest form(){ CreateReturnRequest f=new CreateReturnRequest(); f.setReason(ReturnReason.OTHER); f.setDetail("  damaged   card "); return f; }
 private MultipartFile image(){return new MockMultipartFile("image","proof.png","image/png",new byte[]{1});}
 @Test void createUploadsAndPersistsAfterTransitionAndLockIsFirst(){
   var result=service.createReturnRequest(ORDER_ID,form(),image(),null);
   assertEquals(ReturnStatus.PENDING,result.status()); assertEquals("image-url",result.proofImageUrl());
   InOrder inOrder=inOrder(orders,requests,cloudinary,orderService);
   inOrder.verify(orders).findByIdAndUserIdForUpdate(ORDER_ID,USER_ID);
   inOrder.verify(requests).existsByOrderId(ORDER_ID);
   inOrder.verify(cloudinary).uploadImage(any(),eq("tcgshop/returns"));
   inOrder.verify(orderService).changeStatus(ORDER_ID,OrderStatus.RETURN_REQUESTED,"Lý do khác: damaged card");
   verify(requests).save(argThat(r->r.getOrder()==order&&r.getUser()==order.getUser()&&r.getStatus()==ReturnStatus.PENDING&&r.getReason().equals("Lý do khác: damaged card")&&r.getProofImagePublicId().equals("image-id")&&r.getProofVideoUrl()==null));
 }
 @Test void createUploadsOptionalVideoAndStoresBothProofs(){
   var result=service.createReturnRequest(ORDER_ID,form(),image(),new MockMultipartFile("video","v.mp4","video/mp4",new byte[]{2}));
   assertEquals("image-url",result.proofImageUrl()); assertEquals("video-url",result.proofVideoUrl());
   verify(requests).save(argThat(r->"video-id".equals(r.getProofVideoPublicId())));
   verify(orderService,times(1)).changeStatus(anyLong(),eq(OrderStatus.RETURN_REQUESTED),anyString());
 }
 @Test void missingOwnershipIncludingShopOwnerIsNotFoundWithoutUploads(){
   when(orders.findByIdAndUserIdForUpdate(ORDER_ID,USER_ID)).thenReturn(Optional.empty());
   assertThrows(ResourceNotFoundException.class,()->service.createReturnRequest(ORDER_ID,form(),image(),null));
   when(current.getId()).thenReturn(8L); // Chủ shop vẫn không được xem đơn của người mua qua luồng này.
   when(orders.findByIdAndUserIdForUpdate(ORDER_ID,8L)).thenReturn(Optional.empty());
   assertThrows(ResourceNotFoundException.class,()->service.createReturnRequest(ORDER_ID,form(),image(),null));
   verifyNoInteractions(cloudinary);
 }
 @Test void rejectsDuplicateWrongStatusExpiredAndMissingImageBeforeCloudinary(){
   when(requests.existsByOrderId(ORDER_ID)).thenReturn(true);
   assertEquals("Đơn hàng này đã có yêu cầu trả hàng",assertThrows(BusinessException.class,()->service.createReturnRequest(ORDER_ID,form(),image(),null)).getMessage());
   when(requests.existsByOrderId(ORDER_ID)).thenReturn(false);
   when(requests.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0)); order.setStatus(OrderStatus.SHIPPING);
   assertThrows(BusinessException.class,()->service.createReturnRequest(ORDER_ID,form(),image(),null));
   order.setStatus(OrderStatus.DELIVERED); order.setDeliveredAt(LocalDateTime.now(clock).minusDays(8));
   assertThrows(BusinessException.class,()->service.createReturnRequest(ORDER_ID,form(),image(),null));
   order.setDeliveredAt(LocalDateTime.now(clock));
   assertThrows(BadRequestException.class,()->service.createReturnRequest(ORDER_ID,form(),null,null));
   verifyNoInteractions(cloudinary);
 }
 @Test void rollbackCallbacksDeleteUploadedFilesWhenVideoOrStatusTransitionFails(){
   org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
   when(cloudinary.uploadVideo(any(),anyString())).thenThrow(new IllegalStateException("upload failed"));
   assertThrows(IllegalStateException.class,()->service.createReturnRequest(ORDER_ID,form(),image(),new MockMultipartFile("video","v.mp4","video/mp4",new byte[]{2})));
   var syncs=org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
   syncs.forEach(s->s.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
   verify(cloudinary).delete("image-id","image");
 }
 @Test void statusTransitionFailureDoesNotSaveAndRegistersImageCleanup(){
   org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
   doThrow(new BusinessException("no transition")).when(orderService).changeStatus(anyLong(),any(),anyString());
   assertThrows(BusinessException.class,()->service.createReturnRequest(ORDER_ID,form(),image(),new MockMultipartFile("video","v.mp4","video/mp4",new byte[]{2})));
   assertEquals(0,requests.count()); verify(requests,never()).save(any());
   org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
   verify(cloudinary).delete("image-id","image"); verify(cloudinary).delete("video-id","video");
 }
 @Test void escalationUpdatesRequestPublishesEventAndDoesNotChangeOrder(){
   order.setStatus(OrderStatus.RETURN_REJECTED); ReturnRequest request=new ReturnRequest(); request.setStatus(ReturnStatus.REJECTED); request.setDecidedAt(LocalDateTime.now(clock).minusDays(3));
   when(requests.findByOrderId(ORDER_ID)).thenReturn(Optional.of(request)); EscalateReturnRequest form=new EscalateReturnRequest(); form.setDetail("  I disagree   here ");
   service.escalate(ORDER_ID,form); assertEquals(ReturnStatus.ESCALATED,request.getStatus()); assertEquals("I disagree here",request.getEscalationReason());
   verify(events).publishEvent(any(ReturnRequestEscalatedEvent.class)); verifyNoInteractions(orderService);
 }
 @Test void escalationRejectsEachPrerequisiteAndAcceptsExactDeadline(){
   ReturnRequest request=new ReturnRequest(); request.setStatus(ReturnStatus.REJECTED); request.setDecidedAt(LocalDateTime.now(clock).minusDays(3)); when(requests.findByOrderId(ORDER_ID)).thenReturn(Optional.of(request));
   EscalateReturnRequest f=new EscalateReturnRequest(); f.setDetail("reason is long enough");
   assertThrows(BusinessException.class,()->service.escalate(ORDER_ID,f)); order.setStatus(OrderStatus.RETURN_REJECTED);
   request.setStatus(ReturnStatus.PENDING); assertThrows(BusinessException.class,()->service.escalate(ORDER_ID,f)); request.setStatus(ReturnStatus.REJECTED);
   request.setAdminNote("already"); assertThrows(BusinessException.class,()->service.escalate(ORDER_ID,f)); request.setAdminNote(null);
   request.setDecidedAt(LocalDateTime.now(clock).minusDays(3).minusSeconds(1)); assertThrows(BusinessException.class,()->service.escalate(ORDER_ID,f));
   request.setDecidedAt(LocalDateTime.now(clock).minusDays(3)); assertDoesNotThrow(()->service.escalate(ORDER_ID,f));
 }
 @Test void escalationForSomeoneElsesOrderIsNotFound(){ when(orders.findByIdAndUserIdForUpdate(ORDER_ID,USER_ID)).thenReturn(Optional.empty()); EscalateReturnRequest f=new EscalateReturnRequest(); f.setDetail("reason is long enough"); assertThrows(ResourceNotFoundException.class,()->service.escalate(ORDER_ID,f)); }
 @Test void managerCanRejectEscalationOnlyWithBoundedNote(){
   when(current.isAuthenticated()).thenReturn(true); when(current.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
   order.setStatus(OrderStatus.RETURN_REJECTED); ReturnRequest request=new ReturnRequest(); request.setStatus(ReturnStatus.ESCALATED); when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order)); when(requests.findByOrderId(ORDER_ID)).thenReturn(Optional.of(request));
   assertThrows(BadRequestException.class,()->service.rejectEscalation(ORDER_ID,"  ")); assertThrows(BadRequestException.class,()->service.rejectEscalation(ORDER_ID,"x".repeat(501)));
   service.rejectEscalation(ORDER_ID,"  final note  "); assertEquals(ReturnStatus.REJECTED,request.getStatus()); assertEquals("final note",request.getAdminNote()); assertNotNull(request.getDecidedAt()); verify(events).publishEvent(any(com.tcgshop.event.ReturnEscalationRejectedEvent.class));
 }
 @Test void buyerCannotRejectAndServiceTransactionMetadataIsCorrect(){
   when(current.isAuthenticated()).thenReturn(true); when(current.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false); when(current.hasRole(RoleName.ROLE_ADMIN)).thenReturn(false);
   assertThrows(ForbiddenException.class,()->service.rejectEscalation(ORDER_ID,"note"));
   assertFalse(ReturnRequestServiceImpl.class.getAnnotation(Transactional.class).readOnly());
   Method method=assertDoesNotThrow(()->ReturnRequestServiceImpl.class.getMethod("rejectEscalation",Long.class,String.class));
   assertEquals("hasAnyRole('MANAGER','ADMIN')",method.getAnnotation(PreAuthorize.class).value());
 }
}
