package com.even.video;

import java.io.File;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class CollectionUtils {
	private static final String COLLECTION = "CollectionMusicList"; 
	private static final String COLLECTIONTAG = "TAG";
	/**
	 * 查询收藏列表
	 * @param mMusicName 需要查询的收藏列表
	 */
	public static ArrayList<String> getCollectionMusicList(SharedPreferences mSP,ArrayList<String> mMusicName){
		try {
			mMusicName.clear();
        	String jsonString = VideoMainActivity.mSP.getString(COLLECTIONTAG, "");
    		JSONObject json = new JSONObject(jsonString);
    		JSONArray ja1 = json.getJSONArray(COLLECTION);
    		for(int i = 0; i < ja1.length();i++){
//    			String name = ja1.getString(i);
    			String path = ja1.getString(i);
    			File musicFile = new File(path);
    			if(musicFile.exists()){//判断文件是否存在
    				mMusicName.add(path);
    			}
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
		return mMusicName;
    }

	/**
	 * @param mMusicName 需要保存的收藏列表
	 */
	public static void saveCollectionMusicList(SharedPreferences mSP,ArrayList<String> mMusicName){
		try {
   		 JSONObject jo = new JSONObject();
			 JSONArray jsonarr = new JSONArray();
			 int index = 0;
			 for(String item2 : mMusicName){
//				 jsonarr.put(index, item2.mName);
				 jsonarr.put(index, item2);
				 index++;
			 }
			 jo.put(COLLECTION, jsonarr);
			 mSP.edit().putString(COLLECTIONTAG, jo.toString()).commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * 查询歌曲是否在收藏列表
	 * @param mCurrentAPath 查询的歌曲路径
	 * @param mMusicName 需要查询所在的收藏列表
	 * @return
	 */
	public static boolean itBeenCollected(Context context ,String mCurrentAPath,ArrayList<String> mMusicName){
		try {
			for(String mMusic : mMusicName){
				if(mMusic.equals(mCurrentAPath)){
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 添加歌曲到收藏列表
	 * @param Path 需要收藏的歌曲路径
	 * @param mMusicList 收藏列表
	 */
	public static ArrayList<String> addMusicToCollectionList(String mMusicName,ArrayList<String> mMusicList){
		try {
			mMusicList.add(mMusicName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mMusicList;
	}
	
	/**
	 * 从收藏列表中删除
	 * @param Path 需要删除的歌曲路径
	 * @param mMusicName 收藏列表
	 */
	public static ArrayList<String> removeMusicFromCollectionList(String Path,ArrayList<String> mMusicName){
		try {
			int index = 0;
			for(String mMusic : mMusicName){
				if(mMusic.equals(Path)){
					mMusicName.remove(index);
				}
				index ++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mMusicName;
	}
}
