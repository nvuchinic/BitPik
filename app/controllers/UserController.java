package controllers;

import java.util.ArrayList;
import java.util.List;

import helpers.HashHelper;
import models.*;
import play.*;
import play.data.Form;
import play.db.ebean.Model.Finder;
import play.mvc.*;
import views.html.*;

/**
 * 
 * @author Nermin Graca & Nedzad Hamzic & Neldin Dzekovic
 *
 */
public class UserController extends Controller {

	static Form<User> newUser = new Form<User>(User.class);
	static ArrayList<String> adminList = new ArrayList<String>();
	static String usernameSes;
		
	public static void insertAdmin(String username)	{
		if(!adminList.contains(username)) {
				adminList.add(username);
			}
	}
				
	/**
	 * Either directs to the index.html with the session name already logged in
	 * or directs to the index.html page with "Unknown" as username;
	 * 
	 * @return
	 */
	public static Result index() {
		List<Product> productList = ProductController.findProduct.all();
		List<MainCategory> mainCategoryList = MainCategory.find.all();
		usernameSes = session("username");
		if (usernameSes == null) {
			usernameSes = "";
		} 
		return ok(index.render( usernameSes, productList, adminList, mainCategoryList));
		
	}

	/**
	 * Renders the registration.html page;
	 * 
	 * @return
	 */
	public static Result registration() {
		usernameSes = session("username");
		if (usernameSes == null) {
			usernameSes = "";
		} 
		return ok(registration.render(usernameSes, "", ""));
	}

	/**
	 * Renders the login.html page;
	 * 
	 * @return
	 */
	public static Result login() {
		usernameSes = session("username");
		if (usernameSes == null) {
			usernameSes = "";
		}
		return ok(login.render(usernameSes, "", ""));
		
	}

	/**
	 * 1. Gets the username, password and email from the form from the
	 * Registration.html page; 2. If the username or email is already in our
	 * database, we redirect to the "/registration.html" 3. Creates the User
	 * using the User.create() method; 4. And redirects to the index.html page;
	 * 
	 * @return
	 */
	public static Result addUser() {
		String username = newUser.bindFromRequest().get().username;
		String password = newUser.bindFromRequest().get().password;
		String email = newUser.bindFromRequest().get().email;
		// Unique 'username' verification
		if (User.finder(username) != null) {
			return ok(registration.render("",
					"Username already taken, please choose another one", ""));
		}
		// Unique 'email' verification
		if (User.emailFinder(email)) {
			return ok(registration.render("", "",
					"Email already in use, please choose another one"));
		}
		User.createSaveUser(username, password, email);
		// automatically puts the 'username' created into the session variable;
		session("username", username);
		return redirect("/");

	}

	/**
	 * 1. Gets the username and password from the form from the Login.html page;
	 * 2. The finder() method finds the User with the entered username; assigns
	 * to the User u; 3. If no User has been found, - redirecting to Failed.html
	 * page; 4. If the User has been found - Checks whether the password is
	 * correct; 5. If the password is wrong - redirecting to Failed.html page;
	 * 6. If the password is correct - redirecting to Index.html page; Note*
	 * Store the username in session variable if the Login is successful;
	 * 
	 * @return
	 */
	public static Result findUser() {
		String hashPass;
		String username = newUser.bindFromRequest().get().username;
		String password = newUser.bindFromRequest().get().password;
		User u = User.finder(username);
		if (u == null) {
			return ok(login.render("", "Username nonexisting", ""));
		} else {
			hashPass = u.password;
		}
		boolean userExists = HashHelper.checkPassword(password, hashPass);
		if (userExists == true) {
			// the username put in the session variable under the key
			// "username";
			session("username", username);
			return redirect("/");
		} else {
			return ok(login.render("", "", "Password is wrong"));
		}
	}

	public static Result showProduct(int id)
	{
		usernameSes = session("username");
		Product p = ProductController.findProduct.byId(id);
		return ok(showProduct.render(usernameSes, p));
	}
	
	/**
	 * @author Sanela Grcic & Nermin Graca Method Logout - clears current
	 *         session and redirects to index.html
	 * @return redirect to index.html
	 */
	public static Result logout() {
		session().clear();
		return redirect(routes.UserController.index());
	}

	/**
	 * For the profiles products - that the current logged in user that has published;
	 * The query search for all the products are published by the user logged in;
	 * And renders the profile.html page;
	 * @return renders the profile.html page with the list of products mentioned;
	 */
	public static Result findProfileProducts(){
		usernameSes = session("username");
		if (usernameSes == null) {
			usernameSes = "";
		}
		List <Product> l = ProductController.findProduct.where().eq("owner.username", usernameSes).findList();
		return ok(profile.render(usernameSes, l));
	}
	
	static Finder<Integer, User> findUser = new Finder<Integer, User>(Integer.class, User.class);
	
	/**
	 * Method list all users registered in database
	 * @return
	 */
	public static Result allUsers() {
		usernameSes = session("username");
		if (usernameSes == null) {
			usernameSes = "";
		}
		List<User> userList = findUser.all();
		for (User user: userList)
					{
						if(user.isAdmin)
						{
							insertAdmin(user.username);
						}
					}
		return ok(korisnici.render(usernameSes, userList));
	}
	
	/**
	 * Method shows profile view of single user
	 * @param id
	 * @return
	 */
	public static Result singleUser(int id) {
		usernameSes = session("username");
		if (usernameSes == null) {
			usernameSes = "";
		}
		User u = findUser.byId(id);
		List <Product> l = ProductController.findProduct.where().eq("owner.username", u.username).findList();
		return ok(korisnik.render(usernameSes, u, l));
	}
	
	/**
	 * Deletes the User;
	 * @param id
	 * @return
	 */
	public static Result deleteUser(int id) {
		  User.delete(id);
		  return redirect(routes.UserController.allUsers());
	}
	
	/**
	 * Changes the isAdmin attribute of the user under the given id;
	 * @param id
	 * @return
	 */
	public static Result changeAdmin(int id)
	{
		usernameSes = session("username");
		if (usernameSes == null) {
			usernameSes = "";
		}
		User u = findUser.byId(id);
		//Sets the admin to !true/false;
		u.setAdmin();
		if(u.isAdmin) {
			insertAdmin(u.username);
		} else {
			if (adminList.contains(u.username)) {
				adminList.remove(u.username);
			}
		}
		//
		List <Product> l = ProductController.findProduct.where().eq("owner.username", u.username).findList();
		return ok(korisnik.render(usernameSes, u, l));
	}
	
	public static Result categories(int id) {
		usernameSes = session("username");
		if (usernameSes == null) {
			usernameSes = "";
		}
		List<MainCategory> mainCategoryList = MainCategory.find.all();
		MainCategory mc = MainCategory.findMainCategory(id);
		List<Product> productList = ProductController.findProduct.where().eq("category", mc.name).findList();
		return ok(kategorija.render("", productList, mainCategoryList));
	}
}
