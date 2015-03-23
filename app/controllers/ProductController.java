package controllers;

import helpers.SessionHelper;

import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import models.MainCategory;
import models.Product;
import models.SubCategory;
import models.User;
import views.html.*;
import play.Logger;
import play.data.Form;
import play.db.ebean.Model.Finder;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.i18n.Messages;

import com.google.common.io.Files;


public class ProductController extends Controller {

	static Form<Product> newProduct = new Form<Product>(Product.class);
	static Finder<Integer, Product> findProduct = new Finder<Integer, Product>(Integer.class, Product.class);
	static String usernameSes;

	/**
	 * 
	 * @param id
	 * @return
	 */
	public static Result showProduct(int id) {
		User u = helpers.SessionHelper.getCurrentUser(ctx());
		Product p = ProductController.findProduct.byId(id);
		return ok(showProduct.render(p, u));
	}

	/**
	 * Method takes the usernameSes from the session variable and sends it to
	 * the addProduct.html page; Where the Form for publishing the product needs
	 * to be filled in order to add the Product;
	 */
	public static Result addProduct() {
		
		User currentUser = SessionHelper.getCurrentUser(ctx());
		// 1. Ako nije registrovan da mu oneomogucimo prikaz addProduct.html;
		if (currentUser == null) {
			return redirect(routes.Application.index());
		}
		// 2. Zabrana admin user-u da objavljuje proizvod;
		if (currentUser.isAdmin) {
			return redirect(routes.Application.index());
		}
		List<MainCategory> mainCategoryList = MainCategory.find.all();
		return ok(addProduct.render(mainCategoryList));

	}

	/**
	 * From the filled form takes the necessary values; And creates the object
	 * Product using the constructor; Sends the information about the product
	 * created to the showProduct.html page; Method creates(saves) the product
	 * using the create method from the Product model;
	 * 
	 * @return showProducts.html with the necessary variables;
	 */
	public static Result createProduct() {
		
		String name;
		String desc;
		Double price;
		String mainCategory;
		String subCategory;
		String availability;
		
		try {
			name = newProduct.bindFromRequest().get().name;
			desc = newProduct.bindFromRequest().get().description;
			price = newProduct.bindFromRequest().get().price;
			mainCategory = newProduct.bindFromRequest().get().categoryString;
			subCategory = newProduct.bindFromRequest().get().subCategoryString;
			availability = newProduct.bindFromRequest().get().availability;
		} catch(IllegalStateException e) {
			flash("add_product_null_field", Messages.get("Molimo Vas popunite sva polja u formi."));
			return redirect(routes.ProductController.addProduct());
		}

		MainCategory mc = MainCategory.findMainCategoryByName(mainCategory);
		List<SubCategory> subCats = mc.subCategories;
		Iterator<SubCategory> iter = subCats.iterator();
		SubCategory sc = null;
		while (iter.hasNext()) {
			SubCategory temp = iter.next();
			if (temp.name.equals(subCategory)) {
				sc = temp;
				break;
			}
		}
		User u = SessionHelper.getCurrentUser(ctx());
		Product p = Product.create(name, desc, price, u, mc, sc, availability);
		Logger.of("product").info("User "+ usernameSes +" added a new product '" + p.name + "'");
		return redirect("/addPictureProduct/" + p.id);
	}

	/**
	* Finds the product under the id number; And sends that product to the
	* editProduct.html page on redering; Where we will have a new form that we
	* will edit;
	* 
	* @param id
	* @return
	*/
	public static Result editProduct(int id) {
		 User currentUser = SessionHelper.getCurrentUser(ctx());
	   	 usernameSes = session("username");
	   	 Product p = findProduct.byId(id);
	   	 
	   	 if (p == null) {
	 		Logger.of("product").warn("Failed try to update a product which doesnt exist");
	   		return redirect(routes.Application.index());
	   	 }
	   	 
	   	 List<MainCategory> mainCategoryList = MainCategory.find.all();
	   	 
	   	 //  Ako nije registrovan da mu onemogucimo prikaz editProduct.html;
	   	 if (currentUser == null) {
	   		Logger.of("product").warn("Not registered user tried to update a product");
	   		return redirect("/");
		}
	   			 
	   	if(!currentUser.username.equals(p.owner.username)) {
	   		Logger.of("product").warn(usernameSes + " tried to update an anothers user's product");
  			return redirect(routes.Application.index());
 		}
	   	//  Ako je admin ulogovan, onemogucujemo mu da edituje proizvod;
	   	if (currentUser.isAdmin==true) {
			Logger.of("product").warn("An admin tried to update a users product");
 			return redirect(routes.Application.index());
   		}
   		//  Prosle sve provjere, tj. dozvoljavamo samo registrovanom useru <svog proizvoda> da ga edituje;    
   		return ok(editProduct.render(usernameSes, p, mainCategoryList));
    }

	/**
	* Saves the new values of the attributes that are entered and overwrites
	* over the ones that were entered before;
	* 
	* @param id
	* @return redirect("/showProduct/" + id);
	*/
	public static Result saveEditedProduct(int id) {
		// takes the new attributes that are entered in the form;
		usernameSes = session("username");
		
		String name;
		String desc;
		Double price;
		String mainCategory;
		String subCategory;
		String availability;
		
		try {
			name = newProduct.bindFromRequest().get().name;
			desc = newProduct.bindFromRequest().get().description;
			price = newProduct.bindFromRequest().get().price;
			mainCategory = newProduct.bindFromRequest().get().categoryString;
			subCategory = newProduct.bindFromRequest().get().subCategoryString;
			availability = newProduct.bindFromRequest().get().availability;
		} catch(IllegalStateException e) {		
			flash("edit_product_null_field", Messages.get("Molim Vas popunite sva polja u formi."));
			return redirect(routes.ProductController.editProduct(id));
		}

		MainCategory mc = MainCategory.findMainCategoryByName(mainCategory);
		List<SubCategory> subCats = mc.subCategories;
		Iterator<SubCategory> iter = subCats.iterator();
		SubCategory sc = null;
		while (iter.hasNext()) {
			SubCategory temp = iter.next();
			if (temp.name.equals(subCategory)) {
				sc = temp;
				break;
			}
		}

		// sets all the new entered attributes as the original ones from the
		// product;
		// and saves();
		Product p = findProduct.byId(id);
		String oldname = p.name;
		p.setName(name);
		p.setDesc(desc);
		p.setPrice(price);
		p.setCategory(mc);
		p.setSubCategory(sc);
		p.setAvailability(availability);
		p.save();
		Logger.of("product").info("User "+ usernameSes + " updated the info of product " + oldname + ", NAME : ["+p.name+"]");
		oldname = null;
		flash("edit_product_success", Messages.get("Uspjesno ste izmijenili oglas"));
		return redirect("/showProduct/" + id);	
	}

	/**
	 * Deletes the products found under the given id;
	 * 
	 * @param id
	 * @return
	 */
	public static Result deleteProduct(int id) {
		String toBeDeleted = Product.find.byId(id).name;
		Product.delete(id);
		Logger.of("product").info( session("username") + " deleted the product "+toBeDeleted);
		toBeDeleted = null;
		flash("delete_product_success",  Messages.get("Uspjesno ste izbrisali oglas"));
		return redirect(routes.UserController.findProfileProducts());
	}	
	
	/**
	 * @param id is Product id
	 * @return redirect to html for adding picture
	 */
	public static Result productPicture(int id) {
		   	  	usernameSes = session("username");
			   	 Product p = findProduct.byId(id);
		
		   	 return ok(addPictureProduct.render(usernameSes, p));
		    }
	
	
	
	/**
	 * Uplade image for User profile, and show picture on user /profile.html. 
	 * If file is not image format jpg, jpeg or png redirect user on profile without uploading image.
	 * If file size is bigger then 2MB, redirect user on profile without uploading image.
	 * @return
	 */
	
	public static Result saveFile(int id){
	
		usernameSes = session("username");
		
	   	Product p = findProduct.byId(id);
	   	 		
   	  	//creating path where we are going to save image
		final String savePath = "." + File.separator 
				+ "public" + File.separator + "images" 
				+ File.separator + "productPicture" + File.separator;
		
		//it takes uploaded information  
		MultipartFormData body = request().body().asMultipartFormData();
		FilePart filePart = body.getFile("image");
		File image = filePart.getFile();
		//it takes extension from image that is uploaded
		String extension = filePart.getFilename().substring(filePart.getFilename().lastIndexOf('.'));
		extension.trim();
		
		//If file is not image format jpg, jpeg or png redirect user on profile without uploading image.
		if(	   !extension.equalsIgnoreCase(".jpeg") 
			&& !extension.equalsIgnoreCase(".jpg")
			&& !extension.equalsIgnoreCase(".png") ){
		
			flash("error",  Messages.get("Image type not valid"));
			Logger.of("product").warn( usernameSes + " tried to upload an image that is not valid.");
			return redirect(routes.ProductController.addProduct());
		}
		
		//If file size is bigger then 2MB, redirect user on profile without uploading image.
		double megabyteSize = (image.length() / 1024) / 1024;
		if(megabyteSize > 2){
			flash("error",  Messages.get("Image size not valid"));
			Logger.of("product").warn( usernameSes + " tried to upload an image that is bigger than 2MB.");
			return redirect(routes.ProductController.addProduct());
		}
		
		//creating image name from user id, and take image extension, than move image to new location
		try {
			File profile = new File(savePath + UUID.randomUUID().toString() + extension);
			Files.move(image, profile );		
			String assetsPath = "images" 
					+ File.separator + "productPicture" + File.separator + profile.getName();
			p.productImagePath = assetsPath;
			p.save();
		} catch (IOException e) {
			Logger.of("product").error( usernameSes + " failed to upload an image to the product " +p.name);
			e.printStackTrace();
		}
		flash("add_product_success", Messages.get("Uspjesno ste objavili oglas"));
		return redirect("/showProduct/"+p.id);
	}
	
	/**
	 * If the user does not want to upload a picture.
	 * If we wants to publish the product with no picture.
	 * @param id
	 * @return Result redirect("/showProduct/"+p.id);
	 */
	public static Result saveNoFile(int id){
			Product p = findProduct.byId(id);
			flash("successAddProduct", Messages.get("Uspjesno ste objavili oglas"));
			return redirect("/showProduct/"+p.id);
	}
	
	/**
	 * When a product is bought, the items attribute boolean isSold is set to true;
	 * and the buyer_user is set to the user who is currently logged in, that is, set
	 * to the user who has clicked (later gone through the procedure of the PayPal 
	 * process);
	 * @param product_id
	 * @return
	 */
	public static Result buy_product_success(int product_id) {
		User buyer_user = SessionHelper.getCurrentUser(ctx());
		// kasnije dodati zabrane za admina;
		// zabrane za neregistrovanog usera;
		Product p = findProduct.byId(product_id);
		p.setSold(true);
		p.setBuyer_user(buyer_user);
		p.save();
		
		List <Product> l = ProductController.findProduct.where().eq("owner.username", buyer_user.username).eq("isSold", false).findList();
		
		/*
		 * Provjeriti jel RADI!!!!
		 * Ubaciti
		 * join(ProductController.findProduct.where().eq("isSold", false));
		 * ili sa and();
		 * html zavrsiti!!!
		 */
		Logger.of("product").info("User "+ buyer_user.username +" bought the product '" + p.name + "'");
		return ok(profile.render(l, buyer_user));
	}
	
	/**
	 * When an paypal procedure has failed for some reason (creditcard number wrong or any kind of error occured in the
	 * process), we redirect the user to his profile page, with the list of the products if he has any.
	 * @param product_id
	 * @return we render the .html page : profile.render(l, buyer_user));
	 */
	public static Result buy_product_fail(int product_id) {
		Product p = findProduct.byId(product_id);
		User buyer_user = SessionHelper.getCurrentUser(ctx());
		List <Product> l = ProductController.findProduct.where().eq("owner.username", buyer_user.username).findList();
		Logger.of("product").info("User "+ buyer_user.username +" failed to buy the product '" + p.name + "'");
		return ok(profile.render(l, buyer_user));
	}
	
}
