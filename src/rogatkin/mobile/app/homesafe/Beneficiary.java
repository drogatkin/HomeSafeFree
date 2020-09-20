package rogatkin.mobile.app.homesafe;

import rogatkin.mobile.data.pertusin.PresentA;
import rogatkin.mobile.data.pertusin.StoreA;

public class Beneficiary extends ID {
	@StoreA(unique = true)
	@PresentA(viewFieldName = "@+id/eb_name", listViewFieldName = "@+id/tx_name")
	public String name;
	@StoreA
	@PresentA(viewFieldName = "@+id/eb_address")
	public String email;
	@StoreA
	@PresentA(viewFieldName = "@+id/cb_sms", listViewFieldName = "@+id/im_sms")
	public boolean sendSms;
	@StoreA
	@PresentA(viewFieldName = "@+id/eb_phone")
	public String sms_number;
	@StoreA
	@PresentA(viewFieldName = "@+id/cb_email", listViewFieldName = "@+id/im_email")
	public boolean sendEmail;
	@StoreA
	@PresentA(viewFieldName ="@+id/im_photo", listViewFieldName = "@+id/im_photo", defaultTo="@+drawable/ic_smile")
	public byte[] photo;
	@StoreA
	@PresentA(viewFieldName = "@+id/eb_templ")
	public String safeMessage;
	@StoreA
	public boolean recuring;
	
	@PresentA(listViewFieldName = "@+id/im_linked")
	public boolean linked;

	@Override
	public String toString() {
		return "Beneficiary [id="+id+",name=" + name + ", email=" + email + ", sendSms="
				+ sendSms + ", sms_number=" + sms_number + ", sendEmail="
				+ sendEmail + ", photo=" + photo + ", safeMessage="
				+ safeMessage + ", recuring=" + recuring + ", linked=" + linked
				+ "]";
	}
	
	
	
}
