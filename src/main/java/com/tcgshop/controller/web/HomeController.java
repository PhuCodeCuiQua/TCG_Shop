package com.tcgshop.controller.web;


import com.tcgshop.dto.response.HomePageResponse;
import com.tcgshop.service.HomePageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
	private final HomePageService homePageService;

	@GetMapping("/")
	public String home(Model model) {
		HomePageResponse homePage = homePageService.getHomePage();
		model.addAttribute("games", homePage.games());
		model.addAttribute("bestSellers", homePage.bestSellers());
		model.addAttribute("recentlyViewed", homePage.recentlyViewed());
		model.addAttribute("recommendations", homePage.recommendations());
		model.addAttribute("suppliesAndSealed", homePage.suppliesAndSealed());
		model.addAttribute("latestSets", homePage.latestSets());
		model.addAttribute("heroImages", homePage.heroImages());
		model.addAttribute("featuredShops", homePage.featuredShops());
		return "index";
	}
}
