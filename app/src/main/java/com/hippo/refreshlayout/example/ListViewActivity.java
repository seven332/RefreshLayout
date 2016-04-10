package com.hippo.refreshlayout.example;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.refreshlayout.RefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListViewActivity extends Activity {

    private final int MAX_PAGE = 3;

    private int mPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final List<String> data = new ArrayList<>();
        data.addAll(Arrays.asList(Data.DATA));

        final RefreshLayout refreshLayout = new RefreshLayout(this);
        final ListView listView = new ListView(this);
        refreshLayout.addView(listView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        refreshLayout.setFooterColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN);

        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return data.size();
            }

            @Override
            public Object getItem(int position) {
                return data.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (null == convertView) {
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                ((TextView) convertView).setText(data.get(position));
                return convertView;
            }
        };
        listView.setAdapter(adapter);

        final RefreshLayout.OnRefreshListener onRefreshListener = new RefreshLayout.OnRefreshListener() {
            @Override
            public void onHeaderRefresh() {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        // Wait 3 seconds
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        // Reset page
                        mPage = 0;
                        refreshLayout.setEnableSwipeFooter(true);

                        // Update data
                        data.clear();
                        data.addAll(Arrays.asList(Data.DATA));
                        adapter.notifyDataSetChanged();

                        // Cancel refresh
                        refreshLayout.setHeaderRefreshing(false);
                    }
                }.execute();
            }

            @Override
            public void onFooterRefresh() {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        // Wait 3 seconds
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        // Update page
                        mPage++;
                        if (mPage >= MAX_PAGE) {
                            // If you want to refresh the last page,
                            // comment this line
                            refreshLayout.setEnableSwipeFooter(false);
                        }

                        // Update data
                        data.addAll(Arrays.asList(Data.DATA));
                        adapter.notifyDataSetChanged();

                        // Cancel refresh
                        refreshLayout.setFooterRefreshing(false);
                    }
                }.execute();
            }
        };
        refreshLayout.setOnRefreshListener(onRefreshListener);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Refresh footer immediately when ListView get bottom
                if (!refreshLayout.isRefreshing() && refreshLayout.isAlmostBottom() && mPage < MAX_PAGE) {
                    refreshLayout.setFooterRefreshing(true);
                    onRefreshListener.onFooterRefresh();
                }
            }
        });

        setContentView(refreshLayout);
    }
}
