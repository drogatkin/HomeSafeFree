package rogatkin.mobile.app.homesafe;

import rogatkin.mobile.data.pertusin.PresentA;
import rogatkin.mobile.data.pertusin.StoreA;

public class Home extends HomeAddr { // see android Address
	public Home () {

	}

	public Home (boolean _active) {
		active = _active;
	}
	@StoreA()
	@PresentA(viewFieldName = "@+id/eb_name", listViewFieldName = "@+id/tx_name")
	public String name;
	@StoreA()
	@PresentA(viewFieldName = "@+id/tb_active", listViewFieldName = "@+id/im_it")
	public boolean active;
	@StoreA()
	@PresentA(viewFieldName = "@+id/cb_leave", listViewFieldName = "@+id/im_leave")
	public boolean onLeave;
	@PresentA(listViewFieldName = "@+id/tx_benfs")
	public  int contacts;
}
