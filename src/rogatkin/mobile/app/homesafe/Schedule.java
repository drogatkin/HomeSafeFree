package rogatkin.mobile.app.homesafe;

public class Schedule {
	public int idBenificary;
	public int dayOfWk;
	public boolean weekDay;
	public boolean weekEnd;
	public boolean holiday;
	public int startTime;
	public int endTime;

	boolean match(long time) {
		return false;
	}
}
