# RefreshLayout

RefreshLayout 是基于 `android.support.v4.widget.SwipeRefreshLayout`。用户可以通过上下滑动来刷新数据或者加载更多数据。

The RefreshLayout is forked from `android.support.v4.widget.SwipeRefreshLayout`. The user can refresh data or load more data via a vertical swipe gesture.


# Usage

在最外面的 `build.gradle` 里加上 jitpack，别加到 buildscript 里了。

Add jitpack repository in top `build.gradle`, DO **NOT** ADD IT TO buildscript.

    allprojects {
        repositories {
            ...
            maven { url "https://jitpack.io" }
        }
    }

在项目 `build.gradle` 里添加 RefreshLayout 依赖。

Add RefreshLayout as dependency in project `build.gradle`.

    dependencies {
        ...
        compile 'com.github.seven332:refreshlayout:0.1.0'
    }

在代码中使用：

Use RefreshLayout in your code:

    RefreshLayout.OnRefreshListener onRefreshListener = new RefreshLayout.OnRefreshListener() {
        @Override
        public void onHeaderRefresh() {
            ...
        }
        
        @Override
        public void onFooterRefresh() {
            ...
        }
    }
    refreshLayout.setOnRefreshListener(onRefreshListener);


# License

    Copyright (C) 2015-2016 Hippo Seven

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
