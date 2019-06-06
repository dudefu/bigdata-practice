package com.tools.zookeeper.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class DistributeClient {

    private static final String connectString = "localhost:2181";

    private static final Integer sessionTimeout = 2000;

    private static final String parentNode = "/Servers";

    private ZooKeeper zk = null;

    private volatile List<String> serverList = null;

    private static CountDownLatch countDownLatch = new CountDownLatch(1);

    public void getConnect() throws Exception {
        zk = new ZooKeeper(connectString, sessionTimeout, event -> {
            try {
                log.info("链接成功,准备获取信息");
                countDownLatch.countDown();
                getServerList();
            } catch (Exception e) {
                log.error("获取信息失败！{}", e.getMessage());
            }
        });
        countDownLatch.await();
    }

    public void getServerList() throws Exception {
        /**
         * 读取数据，可以获取到节点列表和节点数据，
         */
//		List<String> children = zk.getChildren(parentNode, true);
        List<String> children = zk.getChildren(parentNode, event -> {
            try {
                /**
                 * 支持自定义Watch，在节点变更时会发送NodeChildrenChanged事件
                 * 不过Watch仅一次有效
                 */
                log.info("此刻有节点变更事件产生！");
                getServerList();
            } catch (Exception e) {
                log.error("注册的Watch调用失败。");
            }
        });

        List<String> list = new ArrayList<>();

        for (String child : children) {
            /**
             * 可以根据路径，获取节点中保存的数据，同样getChildren支持Watch注册
             * 在节点数据发生变化时，可以发送事件。NodeDataChanged
             */
            byte[] data = zk.getData(parentNode + "/" + child, false, null);
            list.add(new String(data));
        }
        serverList = list;
        handlerService();
    }

    public void handlerService() {
        serverList.forEach(server -> log.info("当前在线服务有：{}", server));
    }

    public static void main(String[] args) throws Exception {
        DistributeClient client = new DistributeClient();
        client.getConnect();
        Thread.sleep(Long.MAX_VALUE);
    }

}
