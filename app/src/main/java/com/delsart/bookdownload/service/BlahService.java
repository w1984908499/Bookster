package com.delsart.bookdownload.service;

import android.os.Handler;
import android.os.Message;

import com.delsart.bookdownload.MsgType;
import com.delsart.bookdownload.Url;
import com.delsart.bookdownload.bean.DownloadBean;
import com.delsart.bookdownload.bean.NovelBean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class BlahService extends BaseService {
    private static String TAG = "test";
    private final Handler mHandler;
    private int mPage;
    private String mBaseUrl;
    private CountDownLatch latch;
    private ArrayList<NovelBean> list = new ArrayList<>();

    public BlahService(Handler handler, String keywords) {
        super(handler, keywords);
        this.mHandler = handler;
        mPage = 1;
        mBaseUrl = Url.BLAH + keywords + "&page=";
    }
    String lasts="";
    @Override
    public void get() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    list.clear();
                    Elements select = Jsoup.connect(mBaseUrl + mPage)
                            .timeout(10000)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .userAgent(Url.MOBBILE_AGENT)
                            .get()
                            .select("div.ok-book-item").select("a.okShowInfoModal");
                    latch = new CountDownLatch(select.size());
                    for (int i = 0; i < select.size(); i++) {
                        runInSameTime(select.get(i));
                    }
                    latch.await();
if (select.toString().equals(lasts))
    list.clear();
                    lasts=select.toString();

                    mPage++;
                    Message msg = mHandler.obtainMessage();
                    msg.what = MsgType.SUCCESS;
                    msg.obj = list;
                    mHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = mHandler.obtainMessage();
                    msg.what = MsgType.ERROR;
                    mHandler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void runInSameTime(final Element element) throws IOException {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                String url = element.attr("abs:href");
                try {
                    Document document = Jsoup.connect(url)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .userAgent(Url.MOBBILE_AGENT)
                            .get();

                    String name = document.select("#okBookShow > div.ok-book-base-info > div.row > div.col-sm-8.ok-book-info > div.ok-book-meta > h1").text();
                    String time = "";
                    String info = "";
                    Elements elements = document.select("#okBookShow > div.ok-book-base-info > div.row > div.col-sm-8.ok-book-info > div.ok-book-meta > div.ok-book-desc > div.ok-book-meta-content").select("p");
                    for (int i = 0; i < elements.size(); i++) {


                        if (!elements.get(i).text().equals(""))
                            info = info + elements.get(i).text() + "\n\n";


                    }
                    String category = document.select("#okBookShow > div.ok-book-base-info > div.row > div.col-sm-8.ok-book-info > div.ok-book-meta > div.ok-book-subjects").text();
                    String status = "";
                    String author = document.select("#okBookShow > div.ok-book-base-info > div.row > div.col-sm-8.ok-book-info > div.ok-book-meta > div.row > div > div").text();
                    String words = "";
                    String pic = document.select("#okBookShow > div.ok-book-base-info > div.row > div.col-sm-4 > div > img").attr("abs:src");
                    NovelBean no = new NovelBean(name, time, info, category, status, author, words, pic, url);
                    list.add(no);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                latch.countDown();
            }
        });
    }


    @Override
    public ArrayList<DownloadBean> getDownloadurls(final String url) throws InterruptedException {
        latch = new CountDownLatch(1);
        final ArrayList<DownloadBean> urls = new ArrayList<>();
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                Document document = Jsoup.connect(url)
                            .timeout(10000)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .userAgent(Url.MOBBILE_AGENT)
                            .get();

                    Elements elements = document.select("#okBookShow > div.ok-book-base-info > div.row > div.col-sm-8.ok-book-info > div.ok-book-opt > div > div.col-md-5.ok-book-download > div a");
                    if (elements != null) {
                        for (Element element : elements) {
                            urls.add(new DownloadBean(element.text(), element.attr("abs:href")));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                latch.countDown();
            }
        });
        latch.await();
        return urls;
    }

}
