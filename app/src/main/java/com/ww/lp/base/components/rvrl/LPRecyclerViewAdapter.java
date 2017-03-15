package com.ww.lp.base.components.rvrl;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;
import com.ww.lp.base.R;
import com.ww.lp.base.databinding.RecyclerViewLoadMoreBinding;

import java.util.ArrayList;

import static com.ww.lp.base.components.rvrl.SingleItemClickListener.VIEW_ITEM;
import static com.ww.lp.base.components.rvrl.SingleItemClickListener.VIEW_PROG;

/**
 * RecyclerView Adapter Created by LinkedME06 on 16/11/10.
 */

public class LPRecyclerViewAdapter<E> extends RecyclerView.Adapter {

    public static final int TYPE_NORMAL = 0;  //说明是不带有header的
    public static final int TYPE_HEADER = 2;  //说明是带有Header的

    //剩余几个页面未展示时开始加载更多
    private int visibleThreshold = 2;
    //当前可见数，第一个可见项，所有项
    private int visibleItemCount, firstVisibleItem, totalItemCount;

    //开始页面
    private int pageStartNum = 0;

    //当前页面
    private volatile int pageCurrentNum = pageStartNum;

    //页面总数
    private int pageCount;

    //是否正在加载更多
    private boolean isLoadingMore;
    //数据列表
    private ArrayList<E> mData;
    //item布局
    private int item_layout;
    //databing布局中设置的item对应的variable名称,如：BR.lp_rv_item
    private int item_data_variable;

    //加载更多监听
    private LPRefreshLoadListener.OnLoadMoreListener onLoadMoreListener;
    //刷新监听
    private LPRefreshLoadListener.onRefreshListener onRefreshListener;

    //是否开启下拉刷新功能
    private boolean enableRefresh = true;

    //是否开启加载更多功能
    private boolean enableLoadMore = true;

    private int mHeaderViewId = -1;

    public boolean isEnableLoadMore() {
        return enableLoadMore;
    }

    /**
     * 设置是否开启加载更多
     *
     * @param enableLoadMore true:开启（默认） false:关闭
     */
    public void setEnableLoadMore(boolean enableLoadMore) {
        this.enableLoadMore = enableLoadMore;
    }

    public boolean isEnableRefresh() {
        return enableRefresh;
    }

    /**
     * 是否开启刷新
     *
     * @param enableRefresh true:开启（默认） false:关闭
     */
    public void setEnableRefresh(boolean enableRefresh) {
        this.enableRefresh = enableRefresh;
    }

    public boolean isLoadingMore() {
        return isLoadingMore;
    }

    public void setLoadingMore(boolean loadingMore) {
        isLoadingMore = loadingMore;
    }

    public int getPageStartNum() {
        return pageStartNum;
    }

    public void setPageStartNum(int pageStartNum) {
        this.pageStartNum = pageStartNum;
        this.pageCurrentNum = pageStartNum;
    }

    public int getPageCurrentNum() {
        return pageCurrentNum;
    }

    public void setPageCurrentNum(int pageCurrentNum) {
        this.pageCurrentNum = pageCurrentNum;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public void loadMoreSuccess() {
        pageCurrentNum += 1;
    }

    public int getVisibleThreshold() {
        return visibleThreshold;
    }

    /**
     * 设置加载更多前剩余项数
     *
     * @param visibleThreshold 默认为2
     */
    public void setVisibleThreshold(int visibleThreshold) {
        this.visibleThreshold = visibleThreshold;
    }

    //HeaderView和FooterView的get和set函数
    public int getHeaderViewId() {
        return mHeaderViewId;
    }

    public void setHeaderViewId(final int headerViewId) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mHeaderViewId = headerViewId;
                notifyItemInserted(0);
            }
        });
    }

    private LPHeaderViewHolder lpHeaderViewHolder;

    /**
     * 基本形式，关闭刷新及加载更多功能
     *
     * @param mData       ArrayList数据列表
     * @param item_layout item布局
     */
    public LPRecyclerViewAdapter(ArrayList<E> mData, int item_layout, int item_data_variable) {
        this(mData, item_layout, item_data_variable, null, null);
    }

    public LPRecyclerViewAdapter(ArrayList<E> mData, int item_layout, int item_data_variable, RecyclerView recyclerView) {
        this(mData, item_layout, item_data_variable, null, recyclerView);
    }

    public LPRecyclerViewAdapter(ArrayList<E> mData, int item_layout, int item_data_variable, final ScrollChildSwipeRefreshLayout swipeRefreshLayout, RecyclerView recyclerView) {
        this.mData = mData;
        this.item_layout = item_layout;
        this.item_data_variable = item_data_variable;
        //判断是否开启上拉加载更多
        if (recyclerView != null && enableLoadMore && recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            //垂直滑动
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        return;
                    }
                    visibleItemCount = recyclerView.getChildCount();
                    totalItemCount = linearLayoutManager.getItemCount();
                    firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                    if (!isLoadingMore && pageCount > pageCurrentNum && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                        if (onLoadMoreListener != null) {
                            isLoadingMore = true;
                            Logger.d("onLoadMore() is call-back!");
                            onLoadMoreListener.onLoadMore();
                        }
                    }
                }
            });
        } else {
            enableLoadMore = false;
        }
        //设置刷新监听
        if (swipeRefreshLayout != null && enableRefresh) {
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    //加载更多时不处理刷新操作
                    if (!isLoadingMore() && onRefreshListener != null) {
                        onRefreshListener.onRefresh();
                    } else {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
        } else {
            enableRefresh = false;
        }

//        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        if (viewType == TYPE_HEADER) {
            ViewDataBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), mHeaderViewId, parent, false);
            holder = new LPHeaderViewHolder(binding);
        } else if (viewType == VIEW_ITEM) {
            //data binding
            ViewDataBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), item_layout, parent, false);
            holder = new LPRecyclerViewHolder(binding);
        } else {
            ViewDataBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.recycler_view_load_more, parent, false);
            holder = new LPLoadMoreViewHolder(binding);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LPHeaderViewHolder) {
            setLPHeaderViewHolder((LPHeaderViewHolder) holder);
        } else if (holder instanceof LPRecyclerViewHolder) {
            int actualPostion = position;
            if (mHeaderViewId != -1) {
                actualPostion = actualPostion - 1;
            }
            final E item = mData.get(actualPostion);
            //固定格式，采用databinding设计，因此item的layout中必须包含data variable
            ((LPRecyclerViewHolder) holder).getBinding().setVariable(item_data_variable, item);
            ((LPRecyclerViewHolder) holder).getBinding().executePendingBindings();
        } else {
            ((RecyclerViewLoadMoreBinding) ((LPLoadMoreViewHolder) holder).getBinding()).lpLoadMore.setIndeterminate(true);
        }
    }

    private void setLPHeaderViewHolder(LPHeaderViewHolder holder) {
        this.lpHeaderViewHolder = holder;
    }

    public LPHeaderViewHolder getLPHeaderViewHolder() {
        return lpHeaderViewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        if (mHeaderViewId != -1 && position == 0) {
            return TYPE_HEADER;
        }
        int actualPostion = position;
        if (mHeaderViewId != -1) {
            actualPostion = actualPostion - 1;
        }
        return mData.get(actualPostion) != null ? VIEW_ITEM : VIEW_PROG;
    }

    @Override
    public int getItemCount() {
        if (mHeaderViewId != -1) {
            return mData.size() + 1;
        }
        return mData.size();
    }

    public void setOnLoadMoreListener(LPRefreshLoadListener.OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public void setOnRefreshListener(LPRefreshLoadListener.onRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    /**
     * 设置是否显示加载更多视图
     *
     * @param isProgress true:显示 false:不显示
     */
    public synchronized void showLoadingMore(final boolean isProgress) {
        if (isProgress) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    mData.add(null);
                    int actualSize = mData.size();
                    if (mHeaderViewId != -1) {
                        actualSize = actualSize + 1;
                    }
                    notifyItemInserted(actualSize - 1);
                }
            });
        } else {
            int actualSize = mData.size();
            if (mHeaderViewId != -1) {
                actualSize = actualSize + 1;
            }
            mData.remove(mData.size() - 1);
            notifyItemRemoved(actualSize);
        }
    }

    /**
     * 加载数据项
     */
    public static class LPRecyclerViewHolder extends RecyclerView.ViewHolder {

        private ViewDataBinding binding;

        public LPRecyclerViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ViewDataBinding getBinding() {
            return binding;
        }
    }

    /**
     * 加载更多
     */
    public static class LPLoadMoreViewHolder extends RecyclerView.ViewHolder {

        private ViewDataBinding binding;

        public LPLoadMoreViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ViewDataBinding getBinding() {
            return binding;
        }
    }

    /**
     * 加载更多
     */
    public static class LPHeaderViewHolder extends RecyclerView.ViewHolder {

        private ViewDataBinding binding;

        public LPHeaderViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ViewDataBinding getBinding() {
            return binding;
        }
    }
}
