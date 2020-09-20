package rogatkin.mobile.app.homesafe;

import rogatkin.mobile.data.pertusin.PresentA;
import rogatkin.mobile.data.pertusin.StoreA;

public class ID {
	@StoreA(auto=1,key=true, storeName="_id")
	@PresentA(required = true, viewTagName = "@+id/item_id")
	public long id;
}
