package io.github.shinem.recyclerviewcachedemo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shinem_zhong.
 * @since 2018/12/15
 */
public class DemoListActivity extends AppCompatActivity {
    public static final String EXTRA_USE_PRE_CACHE = "extra_use_pre_cache";

    private final static Object LOADING_HOLDER = new Object();

    private RecyclerView mRecyclerView;
    private List<Object> mDataList = new ArrayList<>();
    private DemoAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private ExtensionCache mExtensionCache;
    private boolean mIsLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_list);
        mRecyclerView = findViewById(R.id.list);
        mAdapter = new DemoAdapter(mDataList);
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setItemPrefetchEnabled(false);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (isScrollingTriggerLoadingMore()) {
                    loadMoreData();
                }
            }
        });
        List dataListForCache = new ArrayList();
        mExtensionCache = new ExtensionCache(
                dataListForCache,
                new DemoAdapter(dataListForCache),
                new ExtensionCache.DataProvider() {
                    @Override
                    public Object getDataAt(int position) {
                        return mDataList.get(position);
                    }
                });

        if (getIntent().getBooleanExtra(EXTRA_USE_PRE_CACHE, false)) {
            // Here is the magic
            mRecyclerView.setViewCacheExtension(mExtensionCache);
        }

        loadMoreData();
    }

    private void loadMoreData() {
        mIsLoading = true;
        final int currentSize = mDataList.size();
        mDataList.add(LOADING_HOLDER);
        mAdapter.notifyItemInserted(mDataList.size());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final List<Object> newDataList = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    newDataList.add(currentSize + i);
                }
                // Cache holders in non-UIThread when loading data.
                mExtensionCache.cacheHoldersForDataList(mRecyclerView, newDataList);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDataList.remove(LOADING_HOLDER);
                        mDataList.addAll(newDataList);
                        mAdapter.notifyDataSetChanged();
                        mIsLoading = false;
                    }
                });
            }
        }).start();
    }

    public static class DemoAdapter extends RecyclerView.Adapter {
        private final List mDataList;

        public DemoAdapter(List dataList) {
            this.mDataList = dataList;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            if (viewType == LOADING_HOLDER.hashCode()) {
                return new LoadingHolder(new FrameLayout(viewGroup.getContext()));
            } else {
                fakeHeavyCompute();
                return new DemoViewHolder(new TextView(viewGroup.getContext()));
            }
        }

        private void fakeHeavyCompute() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int pos) {
            if (viewHolder instanceof DemoViewHolder) {
                DemoViewHolder holder = (DemoViewHolder) viewHolder;
                Object data = mDataList.get(pos);
                // Do not need bind if the target data is already bound in previous pre-caching.
                if (data.equals(holder.data)) {
                    return;
                }
                ((TextView) holder.itemView).setText("Index: " + (int) data);
                holder.data = data;
                fakeHeavyCompute();
            }
        }

        @Override
        public int getItemCount() {
            return mDataList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mDataList.get(position) == LOADING_HOLDER ? LOADING_HOLDER.hashCode() : 0;
        }
    }

    private static class DemoViewHolder extends RecyclerView.ViewHolder {
        private Object data;

        public DemoViewHolder(View view) {
            super(view);
            ((TextView) view).setGravity(Gravity.CENTER);
            ((TextView) view).setTextColor(Color.WHITE);
            view.setBackgroundColor(view.getResources().getColor(R.color.colorAccent));
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400);
            params.topMargin = 20;
            view.setLayoutParams(params);
        }
    }

    private static class LoadingHolder extends RecyclerView.ViewHolder {
        public LoadingHolder(View view) {
            super(view);
            FrameLayout frameLayout = (FrameLayout) view;
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 150));
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            frameLayout.addView(new ProgressBar(view.getContext()), params);
        }
    }

    private boolean isScrollingTriggerLoadingMore() {
        int totalItemCount = mLayoutManager.getItemCount();
        int lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();
        return !mIsLoading
                && totalItemCount > 0
                && (totalItemCount - lastVisibleItemPosition < 3);
    }
}
