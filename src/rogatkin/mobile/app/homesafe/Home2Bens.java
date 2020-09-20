package rogatkin.mobile.app.homesafe;

import rogatkin.mobile.data.pertusin.StoreA;

public class Home2Bens extends ID {
	@StoreA(index=true)
	public long idHome;
	@StoreA(index=true)
	public long idBeneficary;
	@Override
	public String toString() {
		return "Home2Bens [idHome=" + idHome + ", idBeneficary=" + idBeneficary
				+ ", id=" + id + "]";
	}
}
