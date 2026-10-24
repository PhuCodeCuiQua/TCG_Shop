package com.tcgshop.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tcgshop.dto.request.CreateReturnRequest;
import com.tcgshop.dto.request.EscalateReturnRequest;
import com.tcgshop.entity.*;
import com.tcgshop.entity.enums.*;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.repository.*;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.service.impl.ReturnRequestServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Chạy riêng bằng mvnw test -DrunIT=true -Dtest=ReturnRequestIT. */
@SpringBootTest
@EnabledIfSystemProperty(named="runIT",matches="true")
class ReturnRequestIT {
 @Autowired private ReturnRequestService returnService;
 @Autowired private OrderService orderService;
 @Autowired private OrderRepository orderRepository;
 @Autowired private OrderGroupRepository groupRepository;
 @Autowired private OrderItemRepository itemRepository;
 @Autowired private OrderStatusHistoryRepository historyRepository;
 @Autowired private ReturnRequestRepository returnRepository;
 @Autowired private ProductRepository productRepository;
 @Autowired private ShopRepository shopRepository;
 @Autowired private UserRepository userRepository;
 @Autowired private RoleRepository roleRepository;
 @Autowired private JdbcTemplate jdbc;
 @MockBean private CloudinaryService cloudinary;
 private String prefix;
 private User buyer,owner,manager;
 private Shop shop;
 private Product product;
 private OrderGroup group;
 private Order order;
 private final List<Long> rolesCreated=new ArrayList<>();
 @BeforeEach void fixture(){
   prefix="it-"+UUID.randomUUID().toString().replace("-","");
   Role userRole=role(RoleName.ROLE_USER); buyer=user("buyer",userRole); owner=user("owner",userRole,role(RoleName.ROLE_VENDOR)); manager=user("manager",role(RoleName.ROLE_MANAGER));
   shop=new Shop(); shop.setOwner(owner); shop.setName(prefix+"-shop"); shop.setSlug(prefix+"-shop"); shop.setStatus(ShopStatus.APPROVED); shop.setDeleted(false); shop.setCommissionRate(new BigDecimal("5.00")); shop=shopRepository.saveAndFlush(shop);
   product=new Product(); product.setShop(shop); product.setType(ProductType.SUPPLY); product.setName(prefix+"-product"); product.setPrice(new BigDecimal("1000.00")); product.setStock(4); product.setSoldCount(1); product.setStatus(ProductStatus.ACTIVE); product.setDeleted(false); product=productRepository.saveAndFlush(product);
   group=new OrderGroup(); group.setGroupCode(prefix+"-group"); group.setUser(buyer); group.setTotalAmount(new BigDecimal("1000.00")); group.setPaymentMethod(PaymentMethod.COD); group.setPaymentStatus(PaymentStatus.PENDING); group.setIdempotencyToken(UUID.randomUUID().toString()); group=groupRepository.saveAndFlush(group);
   order=new Order(); order.setOrderCode(prefix+"-order"); order.setOrderGroup(group); order.setUser(buyer); order.setShop(shop); order.setStatus(OrderStatus.DELIVERED); order.setSubtotal(new BigDecimal("1000.00")); order.setDiscountAmount(BigDecimal.ZERO); order.setPlatformDiscountAmount(BigDecimal.ZERO); order.setShippingFee(BigDecimal.ZERO); order.setTotalAmount(new BigDecimal("1000.00")); order.setDeliveredAt(LocalDateTime.now()); order.setCommissionRate(new BigDecimal("5.00")); order.setCommissionAmount(new BigDecimal("50.00")); order.setShopRevenue(new BigDecimal("950.00")); order=orderRepository.saveAndFlush(order);
   OrderItem line=new OrderItem(); line.setOrder(order); line.setProduct(product); line.setProductName(product.getName()); line.setPrice(product.getPrice()); line.setQuantity(1); line.setTotalPrice(product.getPrice()); itemRepository.saveAndFlush(line);
   when(cloudinary.uploadImage(any(),anyString())).thenReturn(new UploadResult("https://it/image","it-image")); when(cloudinary.uploadVideo(any(),anyString())).thenReturn(new UploadResult("https://it/video","it-video"));
 }
 private CreateReturnRequest createForm(){CreateReturnRequest f=new CreateReturnRequest();f.setReason(ReturnReason.OTHER);f.setDetail("The card was damaged");return f;}
 private MockMultipartFile image(){return new MockMultipartFile("image","proof.png","image/png",new byte[]{1});}
 private MockMultipartFile video(){return new MockMultipartFile("video","proof.mp4","video/mp4",new byte[]{2});}
 @Test void deliveredOrderCreatesPendingRequestChangesOrderAndWritesHistory(){
   as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),video()));
   Order saved=orderRepository.findById(order.getId()).orElseThrow(); ReturnRequest rr=returnRepository.findByOrderId(order.getId()).orElseThrow();
   assertEquals(OrderStatus.RETURN_REQUESTED,saved.getStatus()); assertEquals(ReturnStatus.PENDING,rr.getStatus()); assertEquals("it-image",rr.getProofImagePublicId()); assertEquals("it-video",rr.getProofVideoPublicId());
   assertTrue(historyRepository.findTimelineByOrderId(order.getId()).stream().anyMatch(h->h.getNewStatus()==OrderStatus.RETURN_REQUESTED));
 }
 @Test void duplicateRequestIsRejected(){as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),null));assertThrows(BusinessException.class,()->as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),null)));}
 @Test void expiredAndUndeliveredOrdersFailBeforeUpload(){
   jdbc.update("update orders set delivered_at = ? where id = ?",LocalDateTime.now().minusDays(8),order.getId());
   assertThrows(BusinessException.class,()->as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),null))); verifyNoInteractions(cloudinary);
   jdbc.update("update orders set status = 'SHIPPING', delivered_at = ? where id = ?",LocalDateTime.now(),order.getId());
   assertThrows(BusinessException.class,()->as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),null))); verifyNoInteractions(cloudinary);
 }
 @Test void simultaneousRequestsCreateOnlyOneReturnRequest() throws Exception {
   int n=2; CountDownLatch ready=new CountDownLatch(n),start=new CountDownLatch(1); ExecutorService pool=Executors.newFixedThreadPool(n);
   List<Future<Boolean>> results=new ArrayList<>(); for(int i=0;i<n;i++) results.add(pool.submit(()->{ready.countDown();start.await();try{as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),null));return true;}catch(BusinessException|org.springframework.dao.DataIntegrityViolationException e){return false;}}));
   assertTrue(ready.await(5,TimeUnit.SECONDS)); start.countDown(); int successes=0; for(Future<Boolean> r:results)if(r.get(20,TimeUnit.SECONDS))successes++; pool.shutdownNow(); assertEquals(1,successes); assertTrue(returnRepository.existsByOrderId(order.getId()));
 }
 @Test void videoUploadFailureRollsBackAndDeletesUploadedImage(){
   when(cloudinary.uploadVideo(any(),anyString())).thenThrow(new IllegalStateException("it-upload"));
   assertThrows(IllegalStateException.class,()->as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),video())));
   assertEquals(0,returnRepository.count()); assertEquals(OrderStatus.DELIVERED,orderRepository.findById(order.getId()).orElseThrow().getStatus()); verify(cloudinary).delete("it-image","image");
 }
 @Test void buyerCanEscalateVendorRejectionOnceAndManagerCanFinishEitherWay(){
   as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),null));
   as(owner,()->orderService.changeStatus(order.getId(),OrderStatus.RETURN_REJECTED,"it-vendor-reason"));
   EscalateReturnRequest form=new EscalateReturnRequest();form.setDetail("Please review this decision");
   as(buyer,()->returnService.escalate(order.getId(),form)); assertEquals(OrderStatus.RETURN_REJECTED,orderRepository.findById(order.getId()).orElseThrow().getStatus()); assertEquals(ReturnStatus.ESCALATED,returnRepository.findByOrderId(order.getId()).orElseThrow().getStatus());
   assertThrows(BusinessException.class,()->as(buyer,()->returnService.escalate(order.getId(),form)));
   as(manager,()->returnService.rejectEscalation(order.getId(),"it-final-manager-note")); ReturnRequest denied=returnRepository.findByOrderId(order.getId()).orElseThrow(); assertEquals(ReturnStatus.REJECTED,denied.getStatus()); assertEquals("it-final-manager-note",denied.getAdminNote());
   assertThrows(BusinessException.class,()->as(buyer,()->returnService.escalate(order.getId(),form)));
 }
 @Test void managerCanApproveEscalationThroughOrderStateMachine(){
   as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),null)); as(owner,()->orderService.changeStatus(order.getId(),OrderStatus.RETURN_REJECTED,"it-rejected"));
   EscalateReturnRequest f=new EscalateReturnRequest();f.setDetail("Please review this decision");as(buyer,()->returnService.escalate(order.getId(),f));
   as(manager,()->orderService.changeStatus(order.getId(),OrderStatus.RETURN_APPROVED,"it-approved")); assertEquals(ReturnStatus.APPROVED,returnRepository.findByOrderId(order.getId()).orElseThrow().getStatus());
 }
 @Test void escalationDeadlineAndManagerAuthorizationAreEnforced(){
   as(buyer,()->returnService.createReturnRequest(order.getId(),createForm(),image(),null));as(owner,()->orderService.changeStatus(order.getId(),OrderStatus.RETURN_REJECTED,"it-rejected"));
   jdbc.update("update return_requests set decided_at = ? where order_id = ?",LocalDateTime.now().minusDays(4),order.getId()); EscalateReturnRequest f=new EscalateReturnRequest();f.setDetail("Please review this decision");
   assertThrows(BusinessException.class,()->as(buyer,()->returnService.escalate(order.getId(),f)));
   assertThrows(AccessDeniedException.class,()->as(buyer,()->returnService.rejectEscalation(order.getId(),"it-note")));
 }
 private Role role(RoleName name){return roleRepository.findByName(name).orElseGet(()->{Role r=new Role();r.setName(name);r.setDescription(prefix+"-role");r=roleRepository.saveAndFlush(r);rolesCreated.add(r.getId());return r;});}
 private User user(String key,Role... roles){User u=new User();u.setEmail(prefix+"-"+key+"@example.test");u.setFullName(prefix+"-"+key);u.setPassword("it-password");u.setStatus(UserStatus.ACTIVE);u.setRoles(Set.of(roles));return userRepository.saveAndFlush(u);}
 private void actAs(User user){CustomUserDetails principal=new CustomUserDetails(userRepository.findByIdWithRoles(user.getId()).orElseThrow());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));}
 private void as(User user,Runnable action){actAs(user);try{action.run();}finally{SecurityContextHolder.clearContext();}}
 @AfterEach void clean(){SecurityContextHolder.clearContext(); if(order!=null&&order.getId()!=null){returnRepository.findByOrderId(order.getId()).ifPresent(r->returnRepository.deleteById(r.getId()));jdbc.update("delete from order_status_histories where order_id = ?",order.getId());jdbc.update("delete from order_items where order_id = ?",order.getId());orderRepository.deleteById(order.getId());} if(group!=null&&group.getId()!=null)groupRepository.deleteById(group.getId());if(product!=null&&product.getId()!=null)productRepository.deleteById(product.getId());if(shop!=null&&shop.getId()!=null)shopRepository.deleteById(shop.getId());for(User u:Arrays.asList(buyer,owner,manager).stream().filter(Objects::nonNull).toList())jdbc.update("delete from user_roles where user_id = ?",u.getId());for(User u:Arrays.asList(buyer,owner,manager).stream().filter(Objects::nonNull).toList())userRepository.deleteById(u.getId());rolesCreated.forEach(roleRepository::deleteById);rolesCreated.clear();}
}
