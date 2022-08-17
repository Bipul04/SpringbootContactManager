const toggleSidebar = () => {

	if ($(".sidebar").is(":visible")) {
		//true
		//bad karna hai
		$(".sidebar").css("display", "none");
		$(".content").css("margin-left", "0%");

	} else {
		//False
		//show karna hai
		$(".sidebar").css("display", "block");
		$(".content").css("margin-left", "20%");
	}
};


//search javascript
const search = () => {

	let query = $("#search-input").val();

	if (query == '') {
		$(".search-result").hide();
	} else {

		//sending request to server

		let url = `http://localhost:8080/search/${query}`;

		fetch(url).then((response) => {
			return response.json();
		})
			.then((data) => {
				//data acessing here...

				let text = `<div class='list-group'>`;

				data.forEach((contact) => {
					text += `<a href='/user/${contact.cId}/contact' class='list-group-item list-group-item-action'> ${contact.name} </a>`
				});

				text += `</div>`;

				$(".search-result").html(text);
				$(".search-result").show();


			});

	}


};


//first request of payment to server to create order
const paymentStart = () => {
	let amount = $("#payment_field").val();
	if (amount == "" || amount == null) {
		// alert("amount is required");
		swal("Error!!", "amount is required", "error");
		return;
	}

	// to send request to sever for creating ordee
	// we will use ajax to send request server to create order

	$.ajax(
		{
			url: "/user/create_order",
			data: JSON.stringify({ amount: amount, info: 'order_request' }),
			contentType: 'application/json',
			type: 'POST',
			dataType: 'json',
			success: function(response) {
				//invoked when success
				if (response.status == "created") {
					//open payment form
					let options = {
						key: "rzp_test_qGNRI25OVjtnU8",
						amount: response.amount,
						currency: "INR",
						name: "Smart Contact Manager",
						description: "Donation",
						image:
							"https://yt3.ggpht.com/-4BGUu55s_ko/AAAAAAAAAAI/AAAAAAAAAAA/3Cfl_C4o8Uo/s108-c-k-c0x00ffffff-no-rj-mo/photo.jpg",
						order_id: response.id,
						handler: function(response) {
							console.log(response.razorpay_payment_id);
							console.log("payment Sucessful");

							//updating order payment done in database before alert message in database
							updatePaymentOnServer(
								response.razorpay_payment_id,
								response.razorpay_order_id,
								"paid"
							);


							// alert("payment Sucess");
							// swal("Done!", "payment Sucess", "success");
						},
						"prefill": {
							"name": "",
							"email": "",
							"contact": ""
						},
						"notes": {
							"address": "Bipul The Hero"
						},
						"theme": {
							"color": "#3399cc"
						}
					};

					let rzp = new Razorpay(options);
					rzp.on('payment.failed', function(response) {
						console.log(response.error.code);
						console.log(response.error.description);
						console.log(response.error.source);
						console.log(response.error.step);
						console.log(response.error.reason);
						console.log(response.error.metadata.order_id);
						console.log(response.error.metadata.payment_id);
						// alert("OOPSS!!payment Failed");
						swal("Error!!", "OOPSS!!payment Failed", "error");

					});
					rzp.open();
				}

			},
			error: function(error) {
				//invoked when Error
				alert("Something went Wrong")
			},

		}
	);

};

//
function updatePaymentOnServer(payment_id,order_id,status){
	$.ajax({

		url: "/user/update_order",
			data: JSON.stringify({ payment_id: payment_id, order_id:order_id,status:status }),
			contentType: 'application/json',
			type: 'POST',
			dataType: 'json',

			success:function (response){
				swal("Done!", "payment Sucess", "success");
			},
			error:function(error){
				//error msg
				swal("Error!!", "your payment is sucessfull but an error in server!!payment Failed", "error");
			}

	});

}