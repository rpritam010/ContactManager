package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	
	// Method for adding common data to response
	
	@ModelAttribute
	public void addCommonData(Model model, Principal principal)
	{
		String userName = principal.getName();
		System.out.println("USERNAME "+userName);
		
		//get the user using username(Email)
		
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER "+user);
		
		model.addAttribute("user", user);
		
	}
	
	// DashBoard Home
	
	@GetMapping("/index")
	public String dashboard(Model model, Principal principal)
	{
		model.addAttribute("title", "User DashBoard");
		return "normal/user_dashboard";
	}
	
	
	//Open Add Form Handler
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model)
	{
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		
		return "normal/add_contact_form";
	}
	
	
	//processing add contact form
	
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session)
	{
		
		try
		{
		
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			
			// Processing and Upload the file
			
			
			if(file.isEmpty())
			{
				// if the file is empty then our message
				
				System.out.println("File is Empty");
				contact.setImage("contact.png");
			}
			else 
			{
				// upload file  to folder and update  the name to contact
				contact.setImage(file.getOriginalFilename());
				
				
				File saveFile = new ClassPathResource("static/image").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image is Uploaded");
				
			}
			
				contact.setUser(user);
				
				user.getContacts().add(contact);
				
				this.userRepository.save(user);
				
				System.out.println("DATA "+contact);
				
				System.out.println("Added to database");
				
				
				//message success..............
				
				session.setAttribute("message", new Message("Your Contact is Added !! Add More....", "success"));
			
			
		
		}catch (Exception e) {
			
			System.out.println("ERROR "+e.getMessage());
			e.printStackTrace();
			
			
			//message error
			
			session.setAttribute("message", new Message("Some Went wrong !! Try Again....", "danger"));
			
		}
		return "normal/add_contact_form";
	}
	
	
	//Show Contacts handler
	
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page")Integer page, Model model, Principal principal)
	{
		model.addAttribute("title","Show User Contacts");
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		
		//current Page-page
		//Contact per page-5
		
		
		Pageable pageable = PageRequest.of(page, 6);
		
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentpage", page);
		model.addAttribute("totalpages", contacts.getTotalPages());
		
		return "normal/show_contact";
	}
	
	
	
	// Showing particular Details
	
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal)
	{
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		
		//
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId())
		{
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		
		return "normal/contact_detail";
	}
	
	
	// Delete Contact Handler
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model,Principal principal, HttpSession session)
	{
		 Contact contact = this.contactRepository.findById(cId).get();
		
		
		//		contact.setUser(null);
		//		
		//		this.contactRepository.delete(contact);
		 
		 User user = this.userRepository.getUserByUserName(principal.getName());
		 
		 user.getContacts().remove(contact);
		 
		 this.userRepository.save(user);
		 
		 
		session.setAttribute("message", new Message("Contact Deleted Succesfully....", "success"));
			
	
		
		return "redirect:/user/show-contacts/0";
	}
	
	
	// Open Update 	Form Handler
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cId, Model model)
	{
		
		model.addAttribute("title", "Update Contact");
		
		Contact contact = this.contactRepository.findById(cId).get();
		
		model.addAttribute("contact", contact);
		
		return "normal/update_form";
	}
	
	
	
	// Update Contact Handler
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, 
			@RequestParam("profileImage") MultipartFile file,
			Model model, HttpSession session, Principal principal)
	{
		try {
			// Old Contact Details
			
			Contact oldcontactDetail = this.contactRepository.findById(contact.getCid()).get();
			
			//Image
			
			if(!file.isEmpty())
			{
				//file work
				
				//Rewrite
				
				// Delete Old Photo
				
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile, oldcontactDetail.getImage());
				file1.delete();
				
				
				
				// Update New Photo
				
				
				File saveFile = new ClassPathResource("static/image").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}else
			{
				contact.setImage(oldcontactDetail.getImage());
			}
			
			User user= this.userRepository.getUserByUserName(principal.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your Contact Is Updated...", "success"));
			
			
		} catch (Exception e) {
			
		}
		
		return "redirect:/user/"+contact.getCid()+"/contact";
	}
	
	
	// Your Profile Handler
	
	
	@GetMapping("/profile")
	public String yourProfile(Model model)
	{
		
		model.addAttribute("title", "profile Page");
		
		return "normal/profile";
	}
}
