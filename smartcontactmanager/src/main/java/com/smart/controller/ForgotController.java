package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;


@Controller
public class ForgotController {

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@RequestMapping("/forgot")
	public String openEmailForm() {
		return "forgot_email_form";
	}

	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam("email") String email, HttpSession session) {

		int otp = (int) (Math.random() * 90000) + 10000;

		String subject = "OTP From SCM";
		String message = "<div style='border:1px solid #e2e2e2;padding:20px'>" + "<h1>" + "OTP is" + "<b>" + otp
				+ "</b>" + "</h1>" + "</div>";
		String to = email;

		boolean flag = emailService.sendEmail(subject, message, to);
		if (flag) {
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		} else {
			session.setAttribute("message", "Chcek Your Email id");
			return "forgot_email_form";
		}

	}

	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session) {
		int yourOtp = (int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");

		if (yourOtp == otp) {
			User userByName = userRepository.getUserByUserName(email);

			if (userByName == null) {
				session.setAttribute("message", "User does not exists with this email");
				return "forgot_email_form";

			} else {
				return "password_change_form";

			}

		} else {
			session.setAttribute("message", "You have entered wrong otp!!");
			return "verify_otp";
		}

	}

	@PostMapping("/password_update")
	public String changedPassword(@RequestParam("password_changed") String password_changed, HttpSession session) {
		String email = (String) session.getAttribute("email");
		User user = userRepository.getUserByUserName(email);

		user.setPassword(bCryptPasswordEncoder.encode(password_changed));

		userRepository.save(user);

		return "redirect:/signin?change=Password Changed Sucessfully";
	}

}