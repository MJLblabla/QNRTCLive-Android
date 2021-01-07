package com.qiniu.droid.rtc.live.demo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.qiniu.droid.rtc.live.demo.R;
import com.qiniu.droid.rtc.live.demo.activity.AudioRoomActivity;
import com.qiniu.droid.rtc.live.demo.activity.PlayingActivity;
import com.qiniu.droid.rtc.live.demo.adapter.LiveRoomListAdapter;
import com.qiniu.droid.rtc.live.demo.model.RoomInfo;
import com.qiniu.droid.rtc.live.demo.model.RoomType;
import com.qiniu.droid.rtc.live.demo.utils.Constants;
import com.qiniu.droid.rtc.live.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.live.demo.utils.ThreadUtils;
import com.qiniu.droid.rtc.live.demo.utils.ToastUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private TextView mEmptyListTip;
    private LiveRoomListAdapter mListAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mRefreshLayout = view.findViewById(R.id.layout_swipe_refresh);
        mRecyclerView = view.findViewById(R.id.rv_home);
        mEmptyListTip = view.findViewById(R.id.tv_empty_list_tip);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mRefreshLayout.setOnRefreshListener(this::requestData);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requestData();
    }

    private void requestData() {
        ThreadUtils.getSingleThreadExecutor().execute(() -> QNAppServer.getInstance().getLivingRooms(new QNAppServer.OnRequestResultCallback() {
            @Override
            public void onRequestSuccess(String responseMsg) {
                parseData(responseMsg);
            }

            @Override
            public void onRequestFailed(int code, String reason) {
                getActivity().runOnUiThread(() ->
                        Log.e(TAG, getString(R.string.toast_get_live_rooms_failed) + " code = " + code + " reason = " + reason));
                parseData("");
            }
        }));
    }

    private void parseData(String responseBody){
        try {
            JSONObject jsonObject = new JSONObject(responseBody);
            String rooms = jsonObject.optString(Constants.KEY_ROOMS);
            if ("null".equals(rooms)) {
                getActivity().runOnUiThread(() -> {
                    ToastUtils.showShortToast(getString(R.string.toast_no_live_rooms));
                    updateListView(new ArrayList<>());
                });
                ToastUtils.showShortToast(getString(R.string.toast_no_live_rooms));
                getActivity().runOnUiThread(() -> updateListView(new ArrayList<>()));
                return;
            }
            List<RoomInfo> roomInfos = new ArrayList<>();
            JSONArray liveRoomArray = new JSONArray(rooms);
            for (int i = 0; i < liveRoomArray.length(); i++) {
                RoomInfo liveRoom = new Gson().fromJson(liveRoomArray.optString(i), RoomInfo.class);
                roomInfos.add(liveRoom);
            }
            getActivity().runOnUiThread(() -> updateListView(roomInfos));
        } catch (JSONException e) {
            e.printStackTrace();
            getActivity().runOnUiThread(() -> updateListView(new ArrayList<>()));
        }
    }

    private void updateListView(List<RoomInfo> roomInfos) {
        if (mListAdapter == null) {
            mListAdapter = new LiveRoomListAdapter(getContext());
            mListAdapter.setOnSelectLiveRoomListener(roomInfo -> {
                Intent intent;
                if (RoomType.VOICE_LIVE.getValue().equals(roomInfo.getStatus())) {
                    intent = new Intent(getContext(), AudioRoomActivity.class);
                    intent.putExtra(Constants.KEY_IS_AUDIO_ANCHOR, false);
                } else {
                    intent = new Intent(getContext(), PlayingActivity.class);
                }
                intent.putExtra(Constants.INTENT_ROOM_INFO, roomInfo);
                startActivity(intent);
            });
            mRecyclerView.setAdapter(mListAdapter);
        }
        mListAdapter.setData(roomInfos);
        mListAdapter.notifyDataSetChanged();
        if (roomInfos.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyListTip.setVisibility(View.VISIBLE);
        } else {
            mEmptyListTip.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        if (mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(false);
        }
    }

}
