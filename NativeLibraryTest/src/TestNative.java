import java.io.File;

public class TestNative {
	
	public static void main(String args[]) throws Exception {
		System.getProperties().list(System.out);
		System.out.println(System.getProperty("os.arch"));
		String osName = System.getProperty("os.name");
		System.out.println(osName);
		String bitSize = System.getProperty("sun.arch.data.model");
		System.out.println(bitSize);
		
		if (osName.equals("Mac OS X"))
			osName = "Darwin";
		
		File library = new File("jni/" + osName + "/" + bitSize + "/libProcessKiller.so").getAbsoluteFile();
		if (!library.exists())
			throw new RuntimeException(library + " doesn't exist");
		System.out.println(library);
		System.load(library.toString());
		System.out.println(whatIsTheAnswer());

	}
	
	public static native int whatIsTheAnswer();

	
}
