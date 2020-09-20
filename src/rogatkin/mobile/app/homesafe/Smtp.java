package rogatkin.mobile.app.homesafe;

import rogatkin.mobile.data.pertusin.PresentA;
import rogatkin.mobile.data.pertusin.StoreA;

public class Smtp {
	@StoreA
	@PresentA(required = true, viewFieldName = "@+id/eb_email")
	public String address;
	@StoreA
	@PresentA(required = true, viewFieldName = "@+id/eb_host")
	public String server;
	@StoreA
	@PresentA(viewFieldName = "@+id/eb_password")
	public String password;
	@StoreA
	@PresentA(viewFieldName = "@+id/eb_port", defaultTo="25")
	public int port;
	@StoreA
	@PresentA(viewFieldName = "@+id/cb_ssl")
	public boolean ssl;

	@StoreA
	@PresentA(viewFieldName = "@+id/cb_auth")
	public boolean oauth;
	
	@StoreA
	@PresentA(viewFieldName = "@+id/eb_timeout")
	public int timeout = 1;

	// not e-mail related vals

	@StoreA
	@PresentA(viewFieldName = "@+id/sp_theme", fillValuesResource="@+array/themes_sel")
	public int theme;
}
