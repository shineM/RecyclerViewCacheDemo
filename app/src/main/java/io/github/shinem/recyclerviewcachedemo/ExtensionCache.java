package io.github.shinem.recyclerviewcachedemo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shinem_zhong.
 * @since 2018/12/15
 */
public class ExtensionCache extends RecyclerView.ViewCacheExtension {

    private final Map<Object, RecyclerView.ViewHolder> mCacheHolders = new ConcurrentHashMap<>();
    // Mirror adapter assurance that getItemViewType and bindViewHolder functions work the same as the origin adapter.
    private final RecyclerView.Adapter mMirrorAdapter;
    // This is only used to bind the cache holder with associated data,
    // which will be clear when next cache order arrive.
    private final List mDataListForBind;

    private final DataProvider mDataProvider;

    public ExtensionCache(List dataListForCache, RecyclerView.Adapter mirrorAdapter, DataProvider mDataProvider) {
        this.mMirrorAdapter = mirrorAdapter;
        this.mDataListForBind = dataListForCache;
        this.mDataProvider = mDataProvider;
    }

    public RecyclerView.ViewHolder getCacheHolder(Object data) {
        RecyclerView.ViewHolder holder = mCacheHolders.get(data);
        return holder;
    }

    public void cacheHoldersForDataList(RecyclerView recyclerView, List<Object> dataList) {
        mDataListForBind.clear();
        mDataListForBind.addAll(dataList);
        mCacheHolders.clear();
        for (Object data : dataList) {
            int position = dataList.indexOf(data);
            RecyclerView.ViewHolder holder = mMirrorAdapter.createViewHolder(
                    recyclerView, mMirrorAdapter.getItemViewType(position));

            // Hack the create process
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            try {
                Field field = RecyclerView.LayoutParams.class.getDeclaredField("mViewHolder");
                field.setAccessible(true);
                field.set(params, holder);
                holder.itemView.setLayoutParams(params);

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

            mMirrorAdapter.bindViewHolder(holder, position);

            // Make the holder state invalid.
            try {
                Method method = RecyclerView.ViewHolder.class.getDeclaredMethod("addFlags", int.class);
                method.setAccessible(true);
                method.invoke(holder, 1 << 2);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

            mCacheHolders.put(data, holder);
        }
    }

    @Nullable
    @Override
    public View getViewForPositionAndType(@NonNull RecyclerView.Recycler recycler, int position, int viewType) {
        RecyclerView.ViewHolder cacheHolder = getCacheHolder(mDataProvider.getDataAt(position));
        return cacheHolder != null ? cacheHolder.itemView : null;
    }

    public interface DataProvider {
        Object getDataAt(int position);
    }
}
