import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class FileRW {
	static FileOutputStream f_out;
	FileInputStream f_in;
	static ObjectOutputStream obj_out;
	ObjectInputStream obj_in;

	public void saveDQT(DistributedQTable dqt) {
		try {
			f_out = new FileOutputStream("qTable.data");
			obj_out = new ObjectOutputStream(f_out);
			obj_out.writeObject(dqt);
			obj_out.close();
			f_out.close();
		} catch (FileNotFoundException e) {
			System.out.println("Could not create save file");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not write to save file");
			e.printStackTrace();
		}
	}
	
	public boolean hasTable() {
		if(new File("dqt.data").exists()) {
			return true;
		}
		return false;
	}

	public DistributedQTable loadTable() {
		// Read from disk using FileInputStream
		try {
			f_in = new FileInputStream("dqt.data");
			obj_in = new ObjectInputStream(f_in);
			Object obj = obj_in.readObject();
			DistributedQTable output = (DistributedQTable) obj;
			return output;
		} catch (FileNotFoundException e) {
			System.out.println("Could not find a save file for qTable");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Found, but could not read save file for qTable");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;	
	}
	
	public boolean hasSaveFile (String imgHash){
		File file = new File(imgHash + ".data");
		if(file.exists()){
			return true;
		}
		return false;
	}
}