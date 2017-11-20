package edu.virginia.cs.gumtreetest;

public class Parent {
	int n = 10;
	
	void test(){
		
	}
	
	public void function1(int j){
		int res = 0;
		try{
			System.out.println("Parent.function1()");
			res = res / n + 1;
		}catch(Exception ex){
			
		}
		for(int i = 0; i < j; i++){
			int k = 0;
			if(n%3 == 1){
				res += n; /* sdssdfds */
			}
			else if(n%3 == 2){
				System.out.println();
			}
			// dsfgsfsafas
		}
		System.out.println(res);
	}

}
