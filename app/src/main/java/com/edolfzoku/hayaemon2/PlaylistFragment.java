/*
 * PlaylistFragment
 *
 * Copyright (c) 2018 Ryota Yamauchi. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.edolfzoku.hayaemon2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.un4seen.bass.BASS;
import com.un4seen.bass.BASS_AAC;
import com.un4seen.bass.BASS_FX;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.app.Activity.RESULT_OK;

public class PlaylistFragment extends Fragment implements View.OnClickListener {
    private ArrayList<String> arPlaylistNames;
    private  ArrayList<ArrayList<SongItem>> arPlaylists;
    private int hFx20K, hFx16K, hFx12_5K, hFx10K, hFx8K, hFx6_3K, hFx5K, hFx4K, hFx3_15K, hFx2_5K, hFx2K, hFx1_6K, hFx1_25K, hFx1K, hFx800, hFx630, hFx500, hFx400, hFx315, hFx250, hFx200, hFx160, hFx125, hFx100, hFx80, hFx63, hFx50, hFx40, hFx31_5, hFx25, hFx20;
    private List<Boolean> arPlayed;
    private RecyclerView recyclerPlaylists;
    private RecyclerView recyclerTab;
    private RecyclerView recyclerSongs;
    private PlaylistsAdapter playlistsAdapter;
    private PlaylistTabAdapter tabAdapter;
    private SongsAdapter songsAdapter;
    private ItemTouchHelper playlistTouchHelper;
    private ItemTouchHelper songTouchHelper;
    private MainActivity activity;
    private int nPlayingPlaylist = -1;
    private int nSelectedPlaylist = 0;
    private int nPlaying;
    private int nSelectedItem;
    private boolean bSorting = false;

    public void setArPlaylists(ArrayList<ArrayList<SongItem>> arLists) { arPlaylists = arLists; }
    public void setArPlaylistNames(ArrayList<String> arNames) { arPlaylistNames = arNames; }
    public int getSelectedPlaylist() { return nSelectedPlaylist; }
    public int getPlaying() { return nPlaying; }
    public int getPlayingPlaylist() { return nPlayingPlaylist; }
    public ItemTouchHelper getPlaylistTouchHelper() { return playlistTouchHelper; }
    public ItemTouchHelper getSongTouchHelper() { return songTouchHelper; }
    public boolean isSorting() { return bSorting; }
    public void setPlayingPlaylist(int nPlaylist) { nPlayingPlaylist = nPlaylist; }
    public int getSongCount(int nPlaylist) { return arPlaylists.get(nPlaylist).size(); }

    public PlaylistFragment()
    {
        activity = null;
        nPlaying = -1;
        arPlaylistNames = new ArrayList<>();
        arPlaylists = new ArrayList<>();
        arPlayed = new ArrayList<>();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context != null && context instanceof MainActivity) {
            activity = (MainActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        activity = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tabAdapter = new PlaylistTabAdapter(activity, R.layout.playlist_tab_item, arPlaylistNames);
        playlistsAdapter = new PlaylistsAdapter(activity, R.layout.playlist_item, arPlaylistNames);
        if(nSelectedPlaylist < arPlaylists.size()) {
            songsAdapter = new SongsAdapter(activity, R.layout.song_item, arPlaylists.get(nSelectedPlaylist));
        }
        else {
            songsAdapter = new SongsAdapter(activity, R.layout.song_item);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btnSortPlaylist)
        {
            if(bSorting)
            {
                recyclerPlaylists.setPadding(0, 0, 0, (int)(80 * getResources().getDisplayMetrics().density + 0.5));
                TextView textAddPlaylist = (TextView) activity.findViewById(R.id.textAddPlaylist);
                textAddPlaylist.setVisibility(View.VISIBLE);
                bSorting = false;
                playlistsAdapter.notifyDataSetChanged();
                Button btnSortPlaylist = (Button) activity.findViewById(R.id.btnSortPlaylist);
                btnSortPlaylist.setText("並べ替え");

                playlistTouchHelper.attachToRecyclerView(null);
            }
            else
            {
                recyclerPlaylists.setPadding(0, 0, 0, 0);
                TextView textAddPlaylist = (TextView) activity.findViewById(R.id.textAddPlaylist);
                textAddPlaylist.setVisibility(View.GONE);
                bSorting = true;
                playlistsAdapter.notifyDataSetChanged();
                Button btnSortPlaylist = (Button) activity.findViewById(R.id.btnSortPlaylist);
                btnSortPlaylist.setText("並べ替えを終了");

                playlistTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(RecyclerView recyclerSongs, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        final int fromPos = viewHolder.getAdapterPosition();
                        final int toPos = target.getAdapterPosition();

                        ArrayList<SongItem> arSongsTemp = arPlaylists.get(fromPos);
                        arPlaylists.remove(fromPos);
                        arPlaylists.add(toPos, arSongsTemp);

                        String strTemp = arPlaylistNames.get(fromPos);
                        arPlaylistNames.remove(fromPos);
                        arPlaylistNames.add(toPos, strTemp);

                        if(fromPos == nPlayingPlaylist) nPlayingPlaylist = toPos;
                        else if(fromPos < nPlayingPlaylist && nPlayingPlaylist <= toPos) nPlayingPlaylist--;
                        else if(fromPos > nPlayingPlaylist && nPlayingPlaylist >= toPos) nPlayingPlaylist++;

                        tabAdapter.notifyItemMoved(fromPos, toPos);
                        playlistsAdapter.notifyItemMoved(fromPos, toPos);

                        return true;
                    }

                    @Override
                    public void clearView(RecyclerView recyclerSongs, RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerSongs, viewHolder);

                        tabAdapter.notifyDataSetChanged();
                        playlistsAdapter.notifyDataSetChanged();

                        SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
                        Gson gson = new Gson();
                        preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
                        preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    }
                });
                playlistTouchHelper.attachToRecyclerView(recyclerPlaylists);
            }
        }
        else if(v.getId() == R.id.textAddPlaylist)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("新しいリストを追加する");
            final EditText editText = new EditText (activity);
            editText.setText("再生リスト");
            builder.setView(editText);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    addPlaylist(editText.getText().toString());
                }
            });
            builder.setNegativeButton("キャンセル", null);
            builder.show();
        }
        else if(v.getId() == R.id.btnRewind)
        {
            if(activity.hStream == 0) return;
            if(BASS.BASS_ChannelBytes2Seconds(activity.hStream, BASS.BASS_ChannelGetPosition(activity.hStream, BASS.BASS_POS_BYTE)) > activity.dLoopA + 1.0)
                BASS.BASS_ChannelSetPosition(activity.hStream, BASS.BASS_ChannelSeconds2Bytes(activity.hStream, activity.dLoopA), BASS.BASS_POS_BYTE);
            else
                playPrev();
        }
        else if(v.getId() == R.id.btnStop)
        {
            if(activity.hStream == 0) return;
            stop();
        }
        else if(v.getId() == R.id.btnPlay)
        {
            if(BASS.BASS_ChannelIsActive(activity.hStream) == BASS.BASS_ACTIVE_PLAYING)
            {
                pause();
            }
            else
            {
                if(BASS.BASS_ChannelIsActive(activity.hStream) == BASS.BASS_ACTIVE_PAUSED)
                {
                    play();
                }
                else
                {
                    if(activity.hStream == 0)
                    {
                        nPlayingPlaylist = nSelectedPlaylist;
                        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
                        arPlayed = new ArrayList<Boolean>();
                        for(int i = 0; i < arSongs.size(); i++)
                            arPlayed.add(false);
                        playNext();
                    }
                }
            }
        }
        else if(v.getId() == R.id.btnForward)
        {
            if(activity.hStream == 0) return;
            playNext();
        }
        else if(v.getId() == R.id.btnPlayMode)
        {
            Button btnPlayMode = (Button)activity.findViewById(R.id.btnPlayMode);
            if(btnPlayMode.getText().equals("通常モード"))
            {
                btnPlayMode.setText("１曲ループ");
                btnPlayMode.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_sloop, 0, 0);
            }
            else if(btnPlayMode.getText().equals("１曲ループ"))
            {
                btnPlayMode.setText("全曲ループ");
                btnPlayMode.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_aloop, 0, 0);
            }
            else if(btnPlayMode.getText().equals("全曲ループ"))
            {
                btnPlayMode.setText("ランダム再生");
                btnPlayMode.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_random, 0, 0);
            }
            else if(btnPlayMode.getText().equals("ランダム再生"))
            {
                btnPlayMode.setText("通常モード");
                btnPlayMode.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_normal, 0, 0);
            }

            SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
            int nPlayMode = 0;
            if(btnPlayMode.getText().equals("通常モード"))
                nPlayMode = 0;
            else if(btnPlayMode.getText().equals("１曲ループ"))
                nPlayMode = 1;
            else if(btnPlayMode.getText().equals("全曲ループ"))
                nPlayMode = 2;
            else if(btnPlayMode.getText().equals("ランダム再生"))
                nPlayMode = 3;
            preferences.edit().putInt("playmode", nPlayMode).commit();
        }
        else if(v.getId() == R.id.textLeft)
        {
            RelativeLayout relativeSongs = (RelativeLayout)activity.findViewById(R.id.relativeSongs);
            relativeSongs.setVisibility(View.INVISIBLE);
            playlistsAdapter.notifyDataSetChanged();
            RelativeLayout relativePlaylists = (RelativeLayout)activity.findViewById(R.id.relativePlaylists);
            relativePlaylists.setVisibility(View.VISIBLE);
        }
        else if(v.getId() == R.id.textAddPlaylist_small)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("新しいリストを追加する");
            final EditText editText = new EditText (activity);
            editText.setText("再生リスト");
            builder.setView(editText);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    addPlaylist(editText.getText().toString());
                }
            });
            builder.setNegativeButton("キャンセル", null);
            builder.show();
        }
        else if(v.getId() == R.id.textAddSong)
        {
            activity.open();
        }
        else if(v.getId() == R.id.textFinishSort)
        {
            recyclerSongs.setPadding(0, 0, 0, (int)(80 * getResources().getDisplayMetrics().density + 0.5));
            TextView textFinishSort = (TextView) activity.findViewById(R.id.textFinishSort);
            textFinishSort.setVisibility(View.GONE);
            TextView textAddSong = (TextView) activity.findViewById(R.id.textAddSong);
            textAddSong.setVisibility(View.VISIBLE);
            bSorting = false;
            songsAdapter.notifyDataSetChanged();
        }
    }

    public void addPlaylist(String strName)
    {
        arPlaylistNames.add(strName);
        ArrayList<SongItem> arSongs = new ArrayList<>();
        arPlaylists.add(arSongs);
        if(activity != null)
        {
            SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
            Gson gson = new Gson();
            preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
            preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
        }
        selectPlaylist(arPlaylists.size() - 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        recyclerPlaylists = (RecyclerView)activity.findViewById(R.id.recyclerPlaylists);
        recyclerPlaylists.setHasFixedSize(false);
        LinearLayoutManager playlistsManager = new LinearLayoutManager(activity);
        recyclerPlaylists.setLayoutManager(playlistsManager);
        recyclerPlaylists.setAdapter(playlistsAdapter);
        recyclerPlaylists.setOnClickListener(this);

        recyclerTab = (RecyclerView)activity.findViewById(R.id.recyclerTab);
        recyclerTab.setHasFixedSize(false);
        LinearLayoutManager tabManager = new LinearLayoutManager(activity);
        tabManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerTab.setLayoutManager(tabManager);
        recyclerTab.setAdapter(tabAdapter);

        recyclerSongs = (RecyclerView)activity.findViewById(R.id.recyclerSongs);
        recyclerSongs.setHasFixedSize(false);
        LinearLayoutManager songsManager = new LinearLayoutManager(activity);
        recyclerSongs.setLayoutManager(songsManager);
        recyclerSongs.setAdapter(songsAdapter);
        recyclerSongs.setOnClickListener(this);
        songTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerSongs, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                final int fromPos = viewHolder.getAdapterPosition();
                final int toPos = target.getAdapterPosition();

                ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
                SongItem itemTemp = arSongs.get(fromPos);
                arSongs.remove(fromPos);
                arSongs.add(toPos, itemTemp);

                if(nPlayingPlaylist == nSelectedPlaylist)
                {
                    Boolean bTemp = arPlayed.get(fromPos);
                    arPlayed.remove(fromPos);
                    arPlayed.add(toPos, bTemp);
                }

                int nStart = fromPos < toPos ? fromPos : toPos;
                for(int i = nStart; i < arSongs.size(); i++) {
                    SongItem songItem = arSongs.get(i);
                    songItem.setNumber(String.format("%d", i+1));
                }

                if(fromPos == nPlaying) nPlaying = toPos;
                else if(fromPos < nPlaying && nPlaying <= toPos) nPlaying--;
                else if(fromPos > nPlaying && nPlaying >= toPos) nPlaying++;

                songsAdapter.notifyItemMoved(fromPos, toPos);

                return true;
            }

            @Override
            public void clearView(RecyclerView recyclerSongs, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerSongs, viewHolder);

                songsAdapter.notifyDataSetChanged();

                SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
                Gson gson = new Gson();
                preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
                preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        songTouchHelper.attachToRecyclerView(recyclerSongs);

        Button btnRewind = (Button) activity.findViewById(R.id.btnRewind);
        btnRewind.setOnClickListener(this);

        Button btnPlay = (Button) activity.findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(this);

        Button btnStop = (Button) activity.findViewById(R.id.btnStop);
        btnStop.setOnClickListener(this);

        Button btnForward = (Button) activity.findViewById(R.id.btnForward);
        btnForward.setOnClickListener(this);

        Button btnPlayMode = (Button) activity.findViewById(R.id.btnPlayMode);
        btnPlayMode.setOnClickListener(this);

        Button btnSortPlaylist = (Button) activity.findViewById(R.id.btnSortPlaylist);
        btnSortPlaylist.setOnClickListener(this);

        TextView textAddPlaylist = (TextView) activity.findViewById(R.id.textAddPlaylist);
        textAddPlaylist.setOnClickListener(this);

        TextView textLeft = (TextView) activity.findViewById(R.id.textLeft);
        textLeft.setOnClickListener(this);

        TextView textAddPlaylist_small = (TextView) activity.findViewById(R.id.textAddPlaylist_small);
        textAddPlaylist_small.setOnClickListener(this);

        TextView textAddSong = (TextView) activity.findViewById(R.id.textAddSong);
        textAddSong.setOnClickListener(this);

        TextView textFinishSort = (TextView) activity.findViewById(R.id.textFinishSort);
        textFinishSort.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1)
        {
            if(resultCode == RESULT_OK)
            {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if(Build.VERSION.SDK_INT < 19)
                {
                    addSong(activity, data.getData());
                }
                else
                {
                    if(data.getClipData() == null)
                    {
                        addSong(activity, data.getData());
                        activity.getContentResolver().takePersistableUriPermission(data.getData(), takeFlags);
                    }
                    else
                    {
                        for(int i = 0; i < data.getClipData().getItemCount(); i++)
                        {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            addSong(activity, uri);
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        }
                    }
                }
                songsAdapter.notifyDataSetChanged();
            }
        }

        SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
        preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info)
    {
        super.onCreateContextMenu(menu, view, info);
        if(view instanceof RelativeLayout)
        {
            RelativeLayout relativeSongs = (RelativeLayout)activity.findViewById(R.id.relativeSongs);
            if(relativeSongs.getVisibility() == View.VISIBLE) {
                RelativeLayout relative = (RelativeLayout)view;
                TextView textNumber = (TextView)relative.getChildAt(0);
                nSelectedItem = Integer.parseInt((String)textNumber.getText()) - 1;
                String strSong = songsAdapter.getTitle(nSelectedItem);
                menu.setHeaderTitle(strSong);
                menu.add("削除");
                if(bSorting) menu.add("並べ替えを終了する");
                else menu.add("曲順の並べ替え");
                menu.add("タイトルとアーティスト名を変更");
            }
            else {
                if(!playlistsAdapter.isClicked()) return;
                playlistsAdapter.setClicked(false);
                int nPosition = playlistsAdapter.getPosition();
                RelativeLayout relative = (RelativeLayout)view;
                menu.setHeaderTitle(playlistsAdapter.getName(nPosition));
                menu.add("再生リスト名を変更");
                menu.add("再生リストを削除");
                menu.add("再生リストを空にする");
            }
        }
        else if(view instanceof TextView)
        {
            int nPosition = tabAdapter.getPosition();
            selectPlaylist(nPosition);
            String strPlaylist = arPlaylistNames.get(nPosition);
            menu.setHeaderTitle(strPlaylist);
            menu.add("再生リスト名を変更");
            menu.add("再生リストを削除");
            menu.add("再生リストを空にする");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        if(item.getTitle().equals("削除"))
        {
            removeSong(nSelectedPlaylist, nSelectedItem);
        }
        else if(item.getTitle().equals("曲順の並べ替え"))
        {
            recyclerSongs.setPadding(0, 0, 0, (int)(64 * getResources().getDisplayMetrics().density + 0.5));
            TextView textFinishSort = (TextView) activity.findViewById(R.id.textFinishSort);
            textFinishSort.setVisibility(View.VISIBLE);
            TextView textAddSong = (TextView) activity.findViewById(R.id.textAddSong);
            textAddSong.setVisibility(View.GONE);
            bSorting = true;
            songsAdapter.notifyDataSetChanged();
        }
        else if(item.getTitle().equals("並べ替えを終了する"))
        {
            bSorting = false;
            songsAdapter.notifyDataSetChanged();
        }
        else if(item.getTitle().equals("タイトルとアーティスト名を変更"))
        {
            ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
            final SongItem songItem = arSongs.get(nSelectedItem);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("タイトルとアーティスト名を変更");
            LinearLayout linearLayout = new LinearLayout(activity);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            final EditText editTitle = new EditText (activity);
            editTitle.setHint("タイトル");
            editTitle.setText(songItem.getTitle());
            final EditText editArtist = new EditText (activity);
            editArtist.setHint("アーティスト名");
            editArtist.setText(songItem.getArtist());
            linearLayout.addView(editTitle);
            linearLayout.addView(editArtist);
            builder.setView(linearLayout);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    songItem.setTitle(editTitle.getText().toString());
                    songItem.setArtist(editArtist.getText().toString());

                    songsAdapter.notifyDataSetChanged();

                    SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
                    Gson gson = new Gson();
                    preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
                    preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
                }
            });
            builder.setNegativeButton("キャンセル", null);
            builder.show();
        }
        else if(item.getTitle().equals("再生リスト名を変更"))
        {
            final int nPlaylist;
            RelativeLayout relativeSongs = (RelativeLayout)activity.findViewById(R.id.relativeSongs);
            if(relativeSongs.getVisibility() == View.VISIBLE)
                nPlaylist = tabAdapter.getPosition();
            else
                nPlaylist = playlistsAdapter.getPosition();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("再生リスト名を変更");
            final EditText editText = new EditText (activity);
            editText.setText(arPlaylistNames.get(nPlaylist));
            builder.setView(editText);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    arPlaylistNames.set(nPlaylist, editText.getText().toString());

                    tabAdapter.notifyDataSetChanged();
                    playlistsAdapter.notifyDataSetChanged();

                    SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
                    Gson gson = new Gson();
                    preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
                    preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
                }
            });
            builder.setNegativeButton("キャンセル", null);
            builder.show();
        }
        else if(item.getTitle().equals("再生リストを削除"))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("再生リストを削除");
            builder.setMessage("再生リストを削除しますが、よろしいでしょうか？");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    int nDelete;
                    RelativeLayout relativeSongs = (RelativeLayout)activity.findViewById(R.id.relativeSongs);
                    if(relativeSongs.getVisibility() == View.VISIBLE)
                        nDelete = tabAdapter.getPosition();
                    else
                        nDelete = playlistsAdapter.getPosition();
                    if(nDelete == nPlayingPlaylist) stop();
                    else if(nDelete < nPlayingPlaylist) nPlayingPlaylist--;
                    arPlaylists.remove(nDelete);
                    arPlaylistNames.remove(nDelete);
                    if(arPlaylists.size() == 0)
                        addPlaylist("再生リスト 1");

                    int nSelect = nDelete;
                    if(nSelect >= arPlaylists.size()) nSelect = arPlaylists.size() - 1;

                    selectPlaylist(nSelect);

                    SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
                    Gson gson = new Gson();
                    preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
                    preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
                }
            });
            builder.setNegativeButton("キャンセル", null);
            builder.show();
        }
        else if(item.getTitle().equals("再生リストを空にする"))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("再生リストを空にする");
            builder.setMessage("再生リストを空にしますが、よろしいでしょうか？");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ArrayList<SongItem> arSongs;
                    RelativeLayout relativeSongs = (RelativeLayout)activity.findViewById(R.id.relativeSongs);
                    if(relativeSongs.getVisibility() == View.VISIBLE)
                        arSongs = arPlaylists.get(tabAdapter.getPosition());
                    else
                        arSongs = arPlaylists.get(playlistsAdapter.getPosition());
                    arSongs.clear();

                    songsAdapter.notifyDataSetChanged();
                    playlistsAdapter.notifyDataSetChanged();

                    SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
                    Gson gson = new Gson();
                    preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
                    preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
                }
            });
            builder.setNegativeButton("キャンセル", null);
            builder.show();
        }
        return super.onContextItemSelected(item);
    }

    public void play()
    {
        if(MainActivity.hStream == 0) return;
        BASS.BASS_ChannelPlay(MainActivity.hStream, false);
        Button btnPlay = (Button)getActivity().findViewById(R.id.btnPlay);
        btnPlay.setText("一時停止");
        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_pause, 0, 0);
        songsAdapter.notifyDataSetChanged();
    }

    public void pause()
    {
        if(MainActivity.hStream == 0) return;
        BASS.BASS_ChannelPause(MainActivity.hStream);
        Button btnPlay = (Button)getActivity().findViewById(R.id.btnPlay);
        btnPlay.setText("再生");
        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_play, 0, 0);
        songsAdapter.notifyDataSetChanged();
    }

    public void playPrev()
    {
        if(MainActivity.hStream == 0) return;
        nPlaying--;
        if(nPlaying < 0) return;
        playSong(nPlaying);
    }

    public void playNext()
    {
        MainActivity activity = (MainActivity)getActivity();
        Button btnPlayMode = (Button)activity.findViewById(R.id.btnPlayMode);
        ArrayList<SongItem> arSongs = arPlaylists.get(nPlayingPlaylist);
        if(btnPlayMode.getText().equals("通常モード") || btnPlayMode.getText().equals("１曲ループ"))
        {
            nPlaying++;
            if(nPlaying >= arSongs.size())
            {
                stop();
                return;
            }
        }
        else if(btnPlayMode.getText().equals("全曲ループ"))
        {
            nPlaying++;
            if(nPlaying >= arSongs.size())
            {
                nPlaying = 0;
            }
        }
        else if(btnPlayMode.getText().equals("ランダム再生"))
        {
            ArrayList<Integer> arTemp = new ArrayList<Integer>();
            for(int i = 0; i < arPlayed.size(); i++)
            {
                if(i == nPlaying) continue;
                Boolean bPlayed = arPlayed.get(i);
                if(!bPlayed.booleanValue())
                {
                    arTemp.add(i);
                }
            }
            if(arTemp.size() == 0)
            {
                for(int i = 0; i < arPlayed.size(); i++)
                {
                    arPlayed.set(i, false);
                }
            }
            if(arPlayed.size() != 1)
            {
                Random random = new Random();
                if(arTemp.size() == 0 || arTemp.size() == arPlayed.size())
                {
                    nPlaying = random.nextInt(arPlayed.size());
                }
                else
                {
                    int nRandom = random.nextInt(arTemp.size());
                    nPlaying = arTemp.get(nRandom);
                }
            }
        }
        playSong(nPlaying);
    }

    public void onPlaylistItemClick(int nPlaylist)
    {
        selectPlaylist(nPlaylist);
        RelativeLayout relativeSongs = (RelativeLayout)activity.findViewById(R.id.relativeSongs);
        relativeSongs.setVisibility(View.VISIBLE);
        RelativeLayout relativePlaylists = (RelativeLayout)activity.findViewById(R.id.relativePlaylists);
        relativePlaylists.setVisibility(View.INVISIBLE);
    }

    public void onSongItemClick(int nSong)
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        if(nPlayingPlaylist == nSelectedPlaylist && nPlaying == nSong)
        {
            if(BASS.BASS_ChannelIsActive(activity.hStream) == BASS.BASS_ACTIVE_PLAYING)
                pause();
            else play();
            return;
        }
        if(nPlayingPlaylist != nSelectedPlaylist) {
            arPlayed = new ArrayList<Boolean>();
            for(int i = 0; i < arSongs.size(); i++)
                arPlayed.add(false);
        }
        nPlayingPlaylist = nSelectedPlaylist;
        playSong(nSong);
    }

    public void playSong(int nSong)
    {
        MainActivity activity = (MainActivity)getActivity();
        activity.clearLoop();
        nPlaying = nSong;
        SongItem item = arPlaylists.get(nPlayingPlaylist).get(nSong);
        String strPath = item.getPath();
        if(MainActivity.hStream != 0)
        {
            BASS.BASS_StreamFree(MainActivity.hStream);
            MainActivity.hStream = 0;
        }
        arPlayed.set(nSong, true);

        BASS.BASS_FILEPROCS fileprocs=new BASS.BASS_FILEPROCS() {
            @Override
            public boolean FILESEEKPROC(long offset, Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    fc.position(offset);
                    return true;
                } catch (IOException e) {
                }
                return false;
            }

            @Override
            public int FILEREADPROC(ByteBuffer buffer, int length, Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    return fc.read(buffer);
                } catch (IOException e) {
                }
                return 0;
            }

            @Override
            public long FILELENPROC(Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    return fc.size();
                } catch (IOException e) {
                }
                return 0;
            }

            @Override
            public void FILECLOSEPROC(Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    fc.close();
                } catch (IOException e) {
                }
            }
        };

        Uri uri = Uri.parse(strPath);
        if(uri.getScheme() != null && uri.getScheme().equals("content")) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            boolean bError = false;
            try {
                mmr.setDataSource(activity.getApplicationContext(), Uri.parse(strPath));
            }
            catch(Exception e) {
                bError = true;
            }
            String strMimeType = null;
            if(!bError)
                strMimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            ContentResolver cr = activity.getApplicationContext().getContentResolver();

            try {
                AssetFileDescriptor afd = cr.openAssetFileDescriptor(Uri.parse(strPath), "r");
                if(afd == null) return;
                FileChannel fc = afd.createInputStream().getChannel();
                if(strMimeType == "audio/mp4")
                    MainActivity.hStream = BASS_AAC.BASS_AAC_StreamCreateFileUser(BASS.STREAMFILE_NOBUFFER, BASS.BASS_STREAM_DECODE, fileprocs, fc);
                else
                    MainActivity.hStream = BASS.BASS_StreamCreateFileUser(BASS.STREAMFILE_NOBUFFER, BASS.BASS_STREAM_DECODE, fileprocs, fc);
            } catch (Exception e) {
                removeSong(nPlayingPlaylist, nPlaying);
                if(nPlaying >= arPlaylists.get(nPlayingPlaylist).size())
                    nPlaying = 0;
                if(arPlaylists.get(nPlayingPlaylist).size() != 0)
                    playSong(nPlaying);
                return;
            }
        }
        else {
            MainActivity.hStream = BASS.BASS_StreamCreateFile(strPath, 0, 0, BASS.BASS_STREAM_DECODE);
        }
        if(MainActivity.hStream == 0) return;

        MainActivity.hStream = BASS_FX.BASS_FX_ReverseCreate(MainActivity.hStream, 2, BASS.BASS_STREAM_DECODE | BASS_FX.BASS_FX_FREESOURCE);
        MainActivity.hStream = BASS_FX.BASS_FX_TempoCreate(MainActivity.hStream, BASS_FX.BASS_FX_FREESOURCE);
        int chan = BASS_FX.BASS_FX_TempoGetSource(MainActivity.hStream);
        BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_FORWARD);
        MainActivity.hFxVol = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_VOLUME, 0);
        hFx20K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx16K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx12_5K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx10K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx8K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx6_3K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx5K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx4K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx3_15K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx2_5K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx2K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx1_6K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx1_25K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx1K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx800 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx630 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx500 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx400 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx315 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx250 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx200 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx160 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx125 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx100 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx80 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx63 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx50 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx40 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx31_5 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx25 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        hFx20 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        ControlFragment controlFragment = (ControlFragment)activity.mSectionsPagerAdapter.getItem(1);
        BASS.BASS_ChannelSetAttribute(MainActivity.hStream, BASS_FX.BASS_ATTRIB_TEMPO, controlFragment.fSpeed);
        BASS.BASS_ChannelSetAttribute(MainActivity.hStream, BASS_FX.BASS_ATTRIB_TEMPO_PITCH, controlFragment.fPitch);
        EqualizerFragment equalizerFragment = (EqualizerFragment)activity.mSectionsPagerAdapter.getItem(3);
        equalizerFragment.setArHFX(new int[] {hFx20K, hFx16K, hFx12_5K, hFx10K, hFx8K, hFx6_3K, hFx5K, hFx4K, hFx3_15K, hFx2_5K, hFx2K, hFx1_6K, hFx1_25K, hFx1K, hFx800, hFx630, hFx500, hFx400, hFx315, hFx250, hFx200, hFx160, hFx125, hFx100, hFx80, hFx63, hFx50, hFx40, hFx31_5, hFx25, hFx20});
        equalizerFragment.setEQ();
        EffectFragment effectFragment = (EffectFragment)activity.mSectionsPagerAdapter.getItem(4);
        effectFragment.applyEffect();
        activity.setSync();
        BASS.BASS_ChannelPlay(MainActivity.hStream, false);
        Button btnPlay = (Button)getActivity().findViewById(R.id.btnPlay);
        btnPlay.setText("一時停止");
        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.ic_pause,0,0);
        LoopFragment loopFragment = (LoopFragment)activity.mSectionsPagerAdapter.getItem(2);
        loopFragment.drawWaveForm(strPath);
        songsAdapter.notifyDataSetChanged();
    }

    public void stop()
    {
        if(MainActivity.hStream == 0) return;
        nPlaying = -1;
        BASS.BASS_ChannelStop(MainActivity.hStream);
        MainActivity.hStream = 0;
        Button btnPlay = (Button)getActivity().findViewById(R.id.btnPlay);
        btnPlay.setText("再生");
        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_play, 0, 0);
        MainActivity activity = (MainActivity)getActivity();
        activity.clearLoop();
        songsAdapter.notifyDataSetChanged();
    }

    public void addSong(MainActivity activity, Uri uri)
    {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        boolean bError = false;
        try {
            mmr.setDataSource(activity.getApplicationContext(), uri);
        }
        catch(Exception e) {
            bError = true;
        }
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        String strTitle = null;
        String strArtist = null;
        if(!bError) {
            strTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            strArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        }
        if(strTitle != null) {
            SongItem item = new SongItem(String.format("%d", arSongs.size()+1), strTitle, strArtist, uri.toString());
            arSongs.add(item);
        }
        else
        {
            strTitle = getFileNameFromUri(activity.getApplicationContext(), uri);
            if(strTitle == null) {
                int startIndex = uri.toString().lastIndexOf('/');
                strTitle = uri.toString().substring(startIndex + 1);
            }
            SongItem item = new SongItem(String.format("%d", arSongs.size()+1), strTitle, "", uri.toString());
            arSongs.add(item);
        }
        arPlayed.add(false);
    }

    public void removeSong(int nPlaylist, int nSong)
    {
        if(nSong < nPlaying) nPlaying--;

        ArrayList<SongItem> arSongs = arPlaylists.get(nPlaylist);
        arSongs.remove(nSong);
        if(nPlaylist == nPlayingPlaylist) arPlayed.remove(nSong);

        for(int i = nSong; i < arSongs.size(); i++) {
            SongItem songItem = arSongs.get(i);
            songItem.setNumber(String.format("%d", i+1));
        }

        songsAdapter.notifyDataSetChanged();

        SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).commit();
        preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).commit();
    }

    public String getFileNameFromUri(Context context, Uri uri) {
        if (null == uri) return null;

        String scheme = uri.getScheme();

        String fileName = null;
        switch (scheme) {
            case "content":
                String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver()
                            .query(uri, projection, null, null, null);
                }
                catch(Exception e) {
                    return null;
                }
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(
                                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                    }
                    cursor.close();
                }
                break;

            case "file":
                fileName = new File(uri.getPath()).getName();
                break;

            default:
                break;
        }
        return fileName;
    }

    public void selectPlaylist(int nSelect)
    {
        if(recyclerTab != null) recyclerTab.scrollToPosition(nSelect);
        nSelectedPlaylist = nSelect;
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelect);
        if(tabAdapter != null) tabAdapter.notifyDataSetChanged();
        if(songsAdapter != null) {
            songsAdapter.changeItems(arSongs);
            songsAdapter.notifyDataSetChanged();
        }
        if(playlistsAdapter != null) playlistsAdapter.notifyDataSetChanged();
    }
}
