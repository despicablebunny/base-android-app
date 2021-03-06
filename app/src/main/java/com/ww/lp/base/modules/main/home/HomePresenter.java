package com.ww.lp.base.modules.main.home;

import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.orhanobut.logger.Logger;
import com.ww.lp.base.CustomApplication;
import com.ww.lp.base.data.ads.AdsListResult;
import com.ww.lp.base.data.project.ProjectInfo;
import com.ww.lp.base.data.project.ProjectListResult;
import com.ww.lp.base.modules.order.list.OrderListActivity;
import com.ww.lp.base.network.ServerImp;
import com.ww.lp.base.network.ServerInterface;
import com.ww.lp.base.utils.SPUtils;
import com.ww.lp.base.utils.ToastUtils;
import com.ww.lp.base.utils.schedulers.BaseSchedulerProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import rx.SingleSubscriber;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by LinkedME06 on 16/11/13.
 */

public class HomePresenter implements HomeContract.Presenter {

    @NonNull
    private String requestTag;

    @NonNull
    private final ServerImp mServerImp;
    @NonNull
    private final HomeContract.View mContractView;
    @NonNull
    private final BaseSchedulerProvider mSchedulerProvider;
    @NonNull
    private CompositeSubscription mSubscriptions;

    public HomePresenter(@NonNull String requestTag, @NonNull ServerImp serverImp,
                         @NonNull HomeContract.View contractView,
                         @NonNull BaseSchedulerProvider schedulerProvider) {
        this.requestTag = requestTag;
        mServerImp = checkNotNull(serverImp, "serverImp cannot be null!");
        mContractView = checkNotNull(contractView, "loginView cannot be null!");
        mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null");

        mSubscriptions = new CompositeSubscription();
        mContractView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        //此处为页面打开后开始加载数据时调用的方法
        loadCarouselImgList();
    }

    @Override
    public void unsubscribe() {
        mSubscriptions.clear();
    }

    /**
     * 请求轮播图列表
     */
    private void loadCarouselImgList() {
        Subscription subscription = mServerImp
                .commonSingle(requestTag, Request.Method.GET, ServerInterface.carousel, null, AdsListResult.class)
                .subscribeOn(mSchedulerProvider.computation())
                .observeOn(mSchedulerProvider.ui())
                .subscribe(new SingleSubscriber<AdsListResult>() {
                    @Override
                    public void onSuccess(AdsListResult adsListResult) {
                        mContractView.updateCarouselView(adsListResult.getData());
                    }

                    @Override
                    public void onError(Throwable error) {
                        ToastUtils.toastError(error);
                        Logger.d(error);
                    }
                });
        mSubscriptions.add(subscription);

    }

    /**
     * 请求项目列表
     */
    @Override
    public void loadProjectList(int pageIndex) {
        // TODO: 16/03/2017 需要陈斌处理不登录也可获取列表
        Map<String, String> params = new HashMap<>();
        params.put("pageIndex", pageIndex + "");
        params.put("isOnlyQueryMyPublis", "0");
        params.put("token", (String) SPUtils.get(CustomApplication.self(), SPUtils.TOKEN, ""));
        Subscription subscription = mServerImp
                .commonSingle(requestTag, Request.Method.POST, ServerInterface.project_list, params, ProjectListResult.class)
                .subscribeOn(mSchedulerProvider.computation())
                .observeOn(mSchedulerProvider.ui())
                .subscribe(new SingleSubscriber<ProjectListResult>() {
                    @Override
                    public void onSuccess(ProjectListResult projectList) {
                        ArrayList<ProjectInfo> arrayList = projectList.getData().getList();
                        mContractView.updateProjectList(true, arrayList, projectList.getData().getPageCount());
                        getPhoneNum();
                    }

                    @Override
                    public void onError(Throwable error) {
                        mContractView.updateProjectList(false, new ArrayList<ProjectInfo>(), 0);
                        ToastUtils.toastError(error);
                        Logger.d(error);
                    }
                });
        mSubscriptions.add(subscription);

    }

    /**
     * 已发布项目列表，目的是为了获取手机号
     */
    public void getPhoneNum() {
        String requestUrl = ServerInterface.project_list;
        Map<String, String> params = new HashMap<>();
        params.put("isOnlyQueryMyPublish", OrderListActivity.PERSONAL);
        params.put("pageIndex", "0");
        params.put("token", (String) SPUtils.get(CustomApplication.self(), SPUtils.TOKEN, ""));
        Subscription subscription = mServerImp
                .commonSingle(requestTag, Request.Method.POST, requestUrl, params, ProjectListResult.class)
                .subscribeOn(mSchedulerProvider.computation())
                .observeOn(mSchedulerProvider.ui())
                .subscribe(new SingleSubscriber<ProjectListResult>() {
                    @Override
                    public void onSuccess(ProjectListResult projectList) {
                        ArrayList<ProjectInfo> arrayList = projectList.getData().getList();
                        if (arrayList.size() > 0) {
                            String phoneNum = arrayList.get(0).getPhoneNum();
                            SPUtils.put(CustomApplication.self(), SPUtils.PHONENUM, phoneNum);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        Logger.d(error);
                    }
                });
        mSubscriptions.add(subscription);

    }


}
