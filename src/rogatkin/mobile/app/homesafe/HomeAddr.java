package rogatkin.mobile.app.homesafe;

import rogatkin.mobile.data.pertusin.PresentA;
import rogatkin.mobile.data.pertusin.StoreA;

public class HomeAddr extends ID {
	@StoreA()
	@PresentA(viewFieldName = "@+id/eb_address")
	public String address;
	@StoreA()
	@PresentA(viewFieldName = "@+id/eb_city", listViewFieldName = "@+id/tx_city")
	public String city;
	@StoreA()
	@PresentA(viewFieldName = "@+id/eb_post")
	public String postCode;
	@StoreA()
	@PresentA(viewFieldName = "@+id/eb_contry")
	public String country;
	@StoreA()
	@PresentA(viewFieldName = "@+id/eb_lon", listViewFieldName = "@+id/tx_lon")
	public double longitude;
	@StoreA()
	@PresentA(viewFieldName = "@+id/eb_lat", listViewFieldName = "@+id/tx_lat")
	public double latitude;
}
