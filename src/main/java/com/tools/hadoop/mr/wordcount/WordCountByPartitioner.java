package com.tools.hadoop.mr.wordcount;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCountByPartitioner {
    /**
     * @Title: main @Description: 定义的driver：封装了mapreduce作业的所有信息 @param @param
     * args @param @throws Exception @return void @throws
     */
    public static void main(String[] args) throws Exception {

        // 设置环境变量HADOOP_USER_NAME，其值是root
        // 在本机调试
        // 读取配置文件
        Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://spark01:9000");
		conf.set("yarn.resourcemanager.hostname", "spark01");


        /**
         * MR压缩相关
         * 在mr中为了减少磁盘和网络io同时可以开启压缩
         */
        //map端压缩
        conf.set("mapreduce.map.output.compress", "true");
        conf.set("mapreduce.map.output.compress.codec", "org.apache.hadoop.io.compress.BZip2Codec");
        //输出端压缩
        conf.set("mapreduce.output.fileoutputformat.compress", "true");
        conf.set("mapreduce.output.fileoutputformat.compress.codec", "org.apache.hadoop.io.compress.BZip2Codec");

        Path out = new Path(args[1]);
        FileSystem fs = FileSystem.get(conf);

        //判断输出路径是否存在，当路径存在时mapreduce会报错
        if (fs.exists(out)) {
            fs.delete(out, true);
            System.out.println("ouput is exit  will delete");
        }

        // 创建任务
        Job job = Job.getInstance(conf, WordCountByPartitioner.class.getName());
        // 设置job的主类
        job.setJarByClass(WordCountByPartitioner.class); // 主类

        // 设置作业的输入路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));

        //设置map的相关参数
        job.setMapperClass(WordCountMapper.class);


        /**
         * 需要注意的事Combiner就是Reducer，他相当于在map端进行的一个reducer，以便于减少网络io
         * - 使用combine时，首先考虑当前MR是否适合combine
         * - 总原则是不论使不使用combine不能影响最终的结果
         * - 在MR时，发生数据倾斜，且可以使用combine时，可以使用combine缓解数据倾斜
         */
        job.setCombinerClass(WordCountReducer.class);

        //设置reduce相关参数
        job.setReducerClass(WordCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        // 设置自定义partitioner
        job.setPartitionerClass(MyPartitioner.class);

        //设置作业的输出路径
        FileOutputFormat.setOutputPath(job, out);

        //设置reduce为4 否则partitioner不会生效
        job.setNumReduceTasks(4);


        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}
