import java.util.Scanner;
import java.io.File;
class A{
	public  void function2(int k){
		for(int i = 0; i < 100; i++){
			Scanner scan = new Scanner(new File(k + i + ".txt"));
			while(scan.hasNext()){
				String token = scan.next();
				int tkn = Integer.parseInt(token);
				if(token == 0){
					System.out.println("token 0");
				}
				else if(token == 1){/**lkjl**/ //hello 
					int x = 0;
				}
				else if(token == 2){
					/**adfs*/k--;//
				}
				else{
					System.out.println("Error");
				}
			}
			scan.close();
		}
	}
	
	
	@Override
	public String toString(){
		return "";
	}
	
	public static void main(String[] args){
		A a = new A();
		a.function1(10);
		System.out.print(a.toString());
	}
}
