package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import com.razorpay.*;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.dao.contactRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;



@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private contactRepository contactRepository;
	
	//my order repository needed
	@Autowired
	private MyOrderRepository myOrderRepository;
	

	// adding common data to response(taking data in model)
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();

		// get the user using username

		User user = userRepository.getUserByUserNamUser(userName);

		model.addAttribute("user", user);

	}

	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {

		model.addAttribute("title", "User DAshboard");

		return "normal/user_dashboard";
	}

	// open add form handler

	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {

		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// process add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {
		try {

			String name = principal.getName();
			User user = this.userRepository.getUserByUserNamUser(name);

			// processing and uploading file

			if (file.isEmpty()) {
				contact.setImage("contact.png");
				// Message

			} else {
				// upload file to folder and upload the name to contact
				contact.setImage(file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/Img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			contact.setUser(user);

			user.getContacts().add(contact);

			this.userRepository.save(user);
			System.out.println("Added to DAtabase");

			// message Sucessful
			session.setAttribute("message", new Message("your Contact is added || add more", "primary"));

		} catch (Exception e) {

			System.out.println("ERROR" + e.getMessage());
			// message error
			session.setAttribute("message", new Message("Something went wrong ,Try again", "danger"));

		}

		return "normal/add_contact_form";

	}

	// show Contact handlere
	// per page = 5[n]
	// current page=0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "this is show contacTs");
		// bhejna hai contact list

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserNamUser(userName);

		// per page = 5[n]
		// current page
		Pageable pageable = PageRequest.of(page, 5);

		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";

	}

	// showing particular contact detail

	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		//

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserNamUser(userName);

		if (user.getId() == contact.getUser().getId()) {

			model.addAttribute("title", contact.getName());

			model.addAttribute("contact", contact);

		}

		return "normal/contact_detail";
	}

	// Delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session,Principal principal) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		// unlink the user from contact
		/*
		  contact.setUser(null);
		  
		  this.contactRepository.delete(contact);
		 */
		
		//getting current user
		User user = this.userRepository.getUserByUserNamUser(principal.getName());
		
		//get all data from user and .remove means deleting all data in contact-bracket one
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		
		session.setAttribute("message", new Message("contact deleted SuceSSfully...", "success"));
		return "redirect:/user/show-contacts/0";
	}

	// open Update Form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cId, Model model) {

		model.addAttribute("title", "this is update form");

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}

	// update-contact handler

	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {

		// model addattribute is for geting all data from form, requestparam is used for
		// getting individual data from form,
		// multipart is used for images getting, model is used for sending data from
		// backend to front end, http session is used for sending message

		System.out.println("Contact name" + contact.getName());

		try {

			// fetching old contact detail to delete old photo
			Contact oldContactDeatil = this.contactRepository.findById(contact.getcId()).get();
			// image........
			if (!file.isEmpty()) {
				// file work...
				// rewrite--naya wala update purana wala delete

				// delete old photo
				File deleteFile = new ClassPathResource("static/Img").getFile();
				File file1 = new File(deleteFile, oldContactDeatil.getImage());
				file1.delete();
				// update new photo
				File saveFile = new ClassPathResource("static/Img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());

			} else {
				contact.setImage(oldContactDeatil.getImage());
			}

			User user = this.userRepository.getUserByUserNamUser(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your contact is update", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/" + contact.getcId() + "/contact";
	}
	
	
	//Your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "my profile");
		return "normal/profile";
		
	}
	
	//open setting handler(open seting tab in chrome)
	
	@GetMapping("/settings")
	public String openSetting() {
		return "normal/settings";
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword,Principal principal,Model model,
			HttpSession session) {
		
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserNamUser(userName);
		
		if (this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			
			//changePAssword
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your password is sucessfully changed", "success"));
			
		}else {
			//error
			session.setAttribute("message", new Message("Your OLd password is Wrong", "danger"));
			
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
		
	}
	
	//cerating order for payment
	
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data,Principal principal) throws Exception {
		
		System.out.println(data);
		int amt = Integer.parseInt(data.get("amount").toString());
		
		var client = new RazorpayClient("rzp_test_qGNRI25OVjtnU8", "6ymKU0X3RLpQ6DXifA4PmBRB");
		
		JSONObject ob = new JSONObject();
		ob.put("amount", amt*100);
		ob.put("currency", "INR");
		ob.put("receipt", "txn_56565");
		
		//creating order(sending request to razorpay server now)
		Order order = client.Orders.create(ob);
		System.out.println(order);
		
		
		//if you want you can save this order details in your  database also
		//saving the order in databse
		
		MyOrder myOrder = new MyOrder();
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUserNamUser(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));
		
		this.myOrderRepository.save(myOrder);
		
		
		
		
		return order.toString();
	}
	
	//handler for updating order payment in database
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data){
		MyOrder myOrder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(myOrder);
		
		return ResponseEntity.ok(Map.of("msg","updated"));
	}
	
	

}
