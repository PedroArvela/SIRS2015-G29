public class LockerRoomApp {

	public static void main(String[] args) {
		Thread listen = new Thread(new PhoneListener());
		listen.start();
		while(true);
	}

}
