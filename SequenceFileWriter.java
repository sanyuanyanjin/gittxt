	/*
	 * Author : Bob 
	 * Create Date : 2016/05/13
	 * Modify Date : 2016/05/26 For Add Mysql JDBC
	 * Describe : Hadoop SeqeunceFile Block Upload(key/value)
	 */
	package com.hadoop.hdfs;

    import java.io.BufferedInputStream;  
    import java.io.FileInputStream;  
    import java.io.IOException;  
    import java.io.InputStream;  
    import java.io.File;  
    import java.net.URI;
    import java.util.ArrayList;
    import java.util.Date;
    
    import java.sql.DriverManager;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.sql.Connection;
    import java.sql.Statement;
    import java.text.DateFormat;
    import java.text.SimpleDateFormat;

    import org.apache.hadoop.conf.Configuration;  
    import org.apache.hadoop.fs.FileSystem;  
    import org.apache.hadoop.fs.Path;  
    import org.apache.hadoop.io.IOUtils;  
    import org.apache.hadoop.io.NullWritable;  
    import org.apache.hadoop.io.SequenceFile;
    import org.apache.hadoop.io.SequenceFile.CompressionType;
    import org.apache.hadoop.io.Text;

    public class SequenceFileWriter{  
        /*
         * File Store
         */
    	private static ArrayList<File> filelist = new ArrayList<File>();
    	private static  String outArgs ="/home/hduser/DicomFile/";
    	private static  String outArgs2 ="hdfs://localhost:9000/output/DicomFile.seq";
    	private static  String outArgs3 ="unnamed";
//    	private static String SaveFolderName =null;
//    	private static String SavePath=null;
    	public static int num =1;
    	/*
    	 * Main Program
    	 */
    	public static void main(String[] args) throws Exception {   
    		//接收程序执行参数
    		if (args.length!=3)
    		{
    			System.out.println("Error : parameters[1] 'UpLoad Path'  may not be blank !!");
    			System.out.println("Error : parameters[2]  'HDFS Path'  may not be blank !!");
    			System.out.println("Error : parameters[3] 'Task Name' parameters[3] may not be blank !!");
    			return;
    		}
    		outArgs =args[0];
    		outArgs2 =args[1];
    		outArgs3 =args[2];
    		//获取文件集
        	getFiles(outArgs);
        	//判断有无上传文件
        	boolean empty =filelist.isEmpty();
        	if (!empty)
        	{
//        		// home/hduser/DicomFile
//        		SaveFolderName =outArgs.substring(1, outArgs.length()-2);
//        		// 4
//        		int size1=SaveFolderName.indexOf("/");
//        		String str ="/";
//        		if (size1>0){
//        			// /home
//        			str=str+SaveFolderName.substring(0, size1);
//        			// hduser/DicomFile
//        			SaveFolderName =outArgs.substring(size1+2, outArgs.length());
//        			//6
//        			int size2=SaveFolderName.indexOf("/");
//        			if (size2>0){
//        				// 16 
//        				int lens =SaveFolderName.length();
//        				// /home/hduser
//        				str=str+"/"+SaveFolderName.substring(0, size2);
//        			}
//        		}
        		UpLoadHDFS(filelist,outArgs);
        	}else{
        		System.out.println("Error : The file path has no files!");
        		return;
        	}
        } 
        /*
         * Get All Files
         */
    	private static void getFiles(String filePath) throws Exception
        {
            	File gzFilesDir = new File(filePath);   
                File[] files = gzFilesDir.listFiles();
            	for ( File file : files )
                {
                    if ( file.isDirectory() )
                    {
                        getFiles( file.getAbsolutePath() );
                    }else{
                    	 filelist.add( file);
                    }
                }
        }
        /*
         * UpLoad HDFS
         */
    	private static void UpLoadHDFS(ArrayList<File>  filelist,String str) throws ClassNotFoundException, IOException, SQLException 
    	{
    		 //数据库链接
    		Connection conn = null;
    		//sql字符串
            String sql; 
          //sql Statement
            PreparedStatement pstmt; 
          //获取任务序号
            int intSeqNo=0; 
          //sql 执行结果
            int s =0; 
            // MySQL的JDBC URL编写方式：jdbc:mysql://主机名称：连接端口/数据库的名称?参数=值
            String url = "jdbc:mysql://localhost:3306/DBPACS?"
                    + "user=root&password=yanjin&useUnicode=true&characterEncoding=UTF8";
            //mysql 链接数据库驱动
            Class.forName("com.mysql.jdbc.Driver");
            //获取数据库链接
            conn = DriverManager.getConnection(url);
            //获取当前日期和时间
            Date date=new Date();
            DateFormat format=new SimpleDateFormat("HH:mm");
            DateFormat format2=new SimpleDateFormat("yyyyMMdd");
            String date8=format2.format(date); 
            String time=format.format(date); 
            //hadoop SequenceFile Writer
    		SequenceFile.Writer writer = null;  
    		//定义HDFS路径
    		String seqFsUrl = outArgs2; 
    		//获取SequenceFile配置信息
    		Configuration conf = new Configuration(); 
    		//获取上传文件流
            FileSystem fs = FileSystem.get(URI.create(seqFsUrl),conf);  
            //获取HDFS文件路径
            Path seqPath = new Path(seqFsUrl);  
            //定义上传文件内容
    		Text value = new Text(); 
    		try
    		{
                //添加数据库事务
                conn.setAutoCommit(false);
                //获取数据库Statement
                Statement stmt = conn.createStatement();
                //获取当前上传任务序号
                sql = "select max(intSeqNo) max from PacsSequenceFileUpLoadInfoTbl";
                // executeQuery会返回结果的集合，否则返回空值
                ResultSet rs = stmt.executeQuery(sql);
                rs.next();
                intSeqNo=rs.getInt(1)+1;
                //关闭数据库返回结果
                rs.close();
                //获取Hadoop SequenceFile Writer
    			writer = SequenceFile.createWriter(fs, conf, seqPath,NullWritable.class, value.getClass(),
    					CompressionType.BLOCK);
    			//根据文件集打包上传HDFS
    			for(int i=0;i<filelist.size();i++)
    			{
    				//获取当前上传文件
    				File file =filelist.get(i);
    				//定义字符串输入流
    				InputStream in = new BufferedInputStream(new FileInputStream(file));  
                    long len = file.length();  
                    byte[] buff = new byte[(int)len];     
                    //文件有内容才上传
                    if ((len = in.read(buff))!= -1) {  
                    	//获得文件内容
                        value.set(buff);  
                        writer.append(NullWritable.get(), value);      
                        //记录文件集信息
                        sql = "INSERT INTO PacsSequenceFileUpLoadNameTbl (intSeqNo,intUpLoadSeqNo,chUpLoadGlobalName)  VALUES (?,?,?)";
                        //定义插入参数
                        pstmt = (PreparedStatement) conn.prepareStatement(sql);
                        pstmt.setInt(1, intSeqNo);
                        pstmt.setInt(2, num);
                        pstmt.setString(3,file.toString().replace(str, ""));
                        //执行Sql语句
                        s = pstmt.executeUpdate();
                        //打印上传文件成功上传
                        System.out.println(file.toString()+" package upLoad successfully.");
                        //清除文件内容
                        value.clear();  
                        //关闭文件流
                        IOUtils.closeStream(in); 
                        //上传序号自动叠加
                        num=num+1;
                  }
    			}
    			//记录上传信息
                sql = "INSERT INTO PacsSequenceFileUpLoadInfoTbl (intSeqNo,chSaveName,chUpLoadDate,chUpLoadTime,chHdfsURL,chUpLoadFileURL)  VALUES (?,?,?,?,?,?)";
                pstmt = (PreparedStatement) conn.prepareStatement(sql);
                pstmt.setInt(1, intSeqNo);
                pstmt.setString(2, outArgs3);
                pstmt.setString(3,date8);
                pstmt.setString(4, time);
                pstmt.setString(5,seqFsUrl);
                pstmt.setString(6,outArgs);
                //执行Sql语句
                s = pstmt.executeUpdate();
                //关闭数据库Statement
                pstmt.close();
                //事务提交
    			conn.commit();
    			//打印总上传文件个数
    			System.out.println("A total of "+filelist.size()+" file package uploaded successfully.");
    		}catch (SQLException e) {
    			//打印错误提示语
    			System.out.println("Error : UpLoad Failed , Check the problem and try again.");
    			//打印错误信息
    			e.printStackTrace();
    			//事务回滚
    			conn.rollback();
    		}finally {
    			//关闭SequenceFile Writer
                IOUtils.closeStream(writer);  
                //关闭数据库链接
                conn.close();
            }
    	}
    }
    	

