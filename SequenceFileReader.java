	/*
	 * Author : Bob 
	 * Date : 2016/05/14
	 * Modify Date : 2016/05/26 For Add Mysql JDBC
	 * Describe : Hadoop SeqeunceFile Block Download(key/value)
	 */

package com.hadoop.hdfs;

import java.io.File;
import java.io.FileWriter;
/*
 * Author : Bob 
 * Date : 2016/05/14
 * Describe : Hadoop SeqeunceFile Block Download(key/value)
 */
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.hadoop.fs.FileSystem;  
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;

public class SequenceFileReader {
	public static int num =1;
	private static  String stroutput =null;
	private static  String outArgs ="hdfs://localhost:9000/output/DicomFile.seq";
	private static  String outArgs2 ="/home/hduser/OutPutFile/";
	private static  String outArgs3 = "1";
	/*
	 * Main Program
	 */
	public static void main(String[] args) throws Exception {   
		//接收程序执行参数
		if (args.length!=3)
		{
			System.out.println("Error : parameters[1] 'HDFS Path'  may not be blank !!");
			System.out.println("Error : parameters[2]  'DownLoad Path'  may not be blank !!");
			System.out.println("Error : parameters[3] 'Task SeqNo' parameters[3] may not be blank !!");
			return;
		}
		outArgs =args[0];
		outArgs2 =args[1];
		outArgs3 =args[2];
		 //数据库链接
		Connection conn = null;
		//sql字符串
        String sql; 
      //获取任务序号
        String FileName=null; 
        // MySQL的JDBC URL编写方式：jdbc:mysql://主机名称：连接端口/数据库的名称?参数=值
        String url = "jdbc:mysql://localhost:3306/DBPACS?"
                + "user=root&password=yanjin&useUnicode=true&characterEncoding=UTF8";
        //mysql 链接数据库驱动
        Class.forName("com.mysql.jdbc.Driver");
        //获取数据库链接
        conn = DriverManager.getConnection(url);
      //获取SequenceFile配置信息
		Configuration conf = new Configuration(); 
		 //获取HDFS文件路径
	    Path seqPath = new Path(outArgs);  
	  //获取下载文件流
	    FileSystem fs = FileSystem.get(URI.create(outArgs), conf);  
	    //定义SequenceFile Reader
	    SequenceFile.Reader reader=null;
	    try{
            //获取数据库Statement
            Statement stmt = conn.createStatement();
            //获取SequenceFile Reader
	    	reader = new SequenceFile.Reader(fs, seqPath, conf);  
	          Writable key = (Writable)  
	            ReflectionUtils.newInstance(reader.getKeyClass(), conf);  
	          Writable value = (Writable) 
	            ReflectionUtils.newInstance(reader.getValueClass(), conf);
	          long position = reader.getPosition();  
	          while (reader.next(key, value)) {   
	            String syncSeen = reader.syncSeen() ? "*" : "";
	            //获取当前下载文件名
	            sql = "SELECT chUpLoadGlobalName  FROM PacsSequenceFileUpLoadNameTbl WHERE intSeqNo=" + outArgs3 +" AND intUpLoadSeqNo="+num;
	            // executeQuery会返回结果的集合，否则返回空值
	            ResultSet rs = stmt.executeQuery(sql);
	            rs.next();
	            FileName=rs.getString(1);
	            //关闭数据库返回结果
	            rs.close();
	            //将文件内容保存在对应文件内
	            SaveFiles(value.toString(),outArgs2+FileName);
	            stroutput="From HDFS download img file : "+outArgs2+FileName +" successfully.";
	            //打印下载文件下载成功
	            System.out.println(stroutput);
	            position = reader.getPosition();
	            //下载序号自动叠加
	            num++;
	          }
	          //打印总下载文件个数
	          num=num-1;
	          System.out.println("A total of "+num+" file download successfully.");
	    }catch (SQLException e) {
			//打印错误提示语
			System.out.println("Error : DownLoad Failed , Check the problem and try again.");
			//打印错误信息
			e.printStackTrace();
	    }finally{
	    	//关闭SequenceFile Reader
	    	IOUtils.closeStream(reader);  
	    	//关闭数据库链接
            conn.close();
	    }
	}
	
	/*
	* DowdLoad All Files
    */
	private static void SaveFiles(String fileContent,String seqOutPutUrl) throws IOException
   {
		int o =0;
		int size =0;
		String path =seqOutPutUrl;
		String str ="/";
		int index=-1;
		while ((index=seqOutPutUrl.indexOf("/",index))>-1){
			++index;
			++o;
		}
		
		for (int k=0;k<=o-1;k++){
			size=path.indexOf("/");
			String ss =path.substring(0,size);
			if (size==0){
				str=str+ss;
				path=path.substring(size+1,path.length());
			}else{
				str=str+ss+"/";
				path=path.substring(size+1,path.length());
			}
		}
		
		
		File file =new File(str);
		
		if (!file.exists()){
			file.mkdirs();
		}
//		if (file.mkdirs()){
			FileWriter fwriter =new FileWriter(seqOutPutUrl);
   			try
   			{
   				fwriter.write(fileContent);
   			}finally
   			{
   				fwriter.flush();
   				fwriter.close();
   			}
//		}
   }
}
