package rogatkin.mobile.app.homesafe;

import rogatkin.mobile.data.pertusin.PresentA;

public class Holder<T> {
	public Holder(T i) {
		holder = i;
	}

	@PresentA(required = true, viewTagName = "@+id/holder")
	public T holder;

	public T get() {
		return holder;
	}
}
