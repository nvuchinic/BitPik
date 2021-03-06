package helpers;

import controllers.routes;
import models.User;
import play.mvc.Result;
import play.mvc.Http.Context;
import play.mvc.Security;

public class AdminFilter extends Security.Authenticator {
	
	/**
	 * gets username from context object for only those users with admin rights
	 */
	@Override
	public String getUsername(Context ctx) {
		if (ctx.session().containsKey("username")) {
			User u = User.finder(ctx.session().get("username"));
			if (u != null && Boolean.TRUE.equals(u.isAdmin)) {
				return u.username;
			}
		}
		return null;
	}

	/**
	 * Redirects unauthorized users to the specified page
	 */
	@Override
	public Result onUnauthorized(Context ctx) {
		return redirect(routes.Application.index());
	}

}