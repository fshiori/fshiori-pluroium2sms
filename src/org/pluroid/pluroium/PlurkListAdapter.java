/*
 * Copyright (C) 2011 The Pluroium Development Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pluroid.pluroium;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.pluroid.pluroium.data.PlurkListItem;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PlurkListAdapter extends BaseAdapter {

    private static final String TAG = "PlurkListAdapter";
    private LayoutInflater inflater;
    private Vector<PlurkListItem> plurks;
    private Context context;
    
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
    
    class AvatarViewObject {
        ImageView imageView;
        Bitmap avatar;
    }
    
    private static class ViewHolder {
        ImageView avatar;
        ImageView lock;
        TextView displayname;
        TextView qualifier;
        TextView content;
        TextView responses;
        TextView favorites;
        TextView posted;
    }
    
    public PlurkListAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);    
        plurks = new Vector<PlurkListItem>();        
    }
    
    public int getCount() {
        return plurks.size();
    }

    public PlurkListItem getItem(int index) {
    	PlurkListItem ret = null;
    	if (index < plurks.size()) {
    		ret = plurks.get(index);
    	}
        return ret;
    }

    public long getItemId(int item) {
    	long id = 0;
    	if (item < plurks.size()) {
    		id = plurks.get(item).getPlurkId();
    	}
        return id;
    }

    public View getView(int index, View convertView, ViewGroup parent) {
        final int ind = index;
        ViewHolder holder;
        final ListView parentList = (ListView) parent;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.plurk_list_item, null);
            holder = new ViewHolder();
            
            holder.avatar = (ImageView) convertView.findViewById(R.id.plurk_item_avatar);
            holder.lock = (ImageView) convertView.findViewById(R.id.plurk_item_lock);
            holder.displayname = (TextView) convertView.findViewById(R.id.plurk_item_displayname);
            holder.qualifier = (TextView) convertView.findViewById(R.id.plurk_item_qualifier);
            holder.content = (TextView) convertView.findViewById(R.id.plurk_item_content);
            
            holder.posted = (TextView) convertView.findViewById(R.id.plurk_item_posted);
            holder.favorites = (TextView) convertView.findViewById(R.id.plurk_favorite_count);
            holder.responses = (TextView) convertView.findViewById(R.id.plurk_response_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final PlurkListItem item = plurks.get(index);
        final ImageView iv = holder.avatar;
        holder.avatar.setImageResource(R.drawable.avatar_unknown_medium);
        new FetchAvatarTask().execute(iv, String.valueOf(item.getUserId()), item.getAvatarIndex());
        
        // display name
        String name = item.getNickname();
        holder.displayname.setText(name);
        
        // qualifier
        String qT = item.getQualifierTranslated();
        String qualifier = item.getQualifier();
        Resources res = context.getResources();

        int colorId = res.getIdentifier("qualifier_" + qualifier, "color", "org.pluroid.pluroium");

        holder.qualifier.setText(qT);
        if (colorId > 0) {
            holder.qualifier.setTextColor(Color.WHITE);
            holder.qualifier.setBackgroundColor(res.getColor(colorId));
        } else {
            holder.qualifier.setTextColor(Color.BLACK);
            holder.qualifier.setBackgroundColor(Color.TRANSPARENT);
        }
        
        holder.content.setText(item.getRawContent());
        
        int favorites = item.getFavorites();
        
        holder.favorites.setText(String.valueOf(favorites));
        if (favorites == 0) {
        	holder.favorites.setTextAppearance(context, R.style.MetaContentText);
        } else {
        	holder.favorites.setTextAppearance(context, R.style.FavoritesCountText);
        }
        
        holder.responses.setText(String.valueOf(item.getResponses()));
        
        if (item.getHasSeen() == 0) {
            holder.responses.setTextAppearance(context, R.style.ResponsesCountText);
        } else {
            holder.responses.setTextAppearance(context, R.style.MetaContentText);
        }
        
        convertView.setClickable(true);
        convertView.setFocusable(true);
        convertView.setBackgroundResource(android.R.drawable.menuitem_background);
        convertView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                parentList.performItemClick(parentList, ind, item.getPlurkId());
            }
            
        });
        convertView.setLongClickable(true);
        
        holder.posted.setText(sdf.format(item.getPosted()));
        String limit = item.getLimitTo();
        holder.lock.setVisibility(limit.length() > 0 ? View.VISIBLE : View.INVISIBLE);

        return convertView;
    }
    
    public void addPlurks(List<PlurkListItem> plurks) {
        this.plurks.addAll(plurks);
        notifyDataSetChanged();
    }
    
    public void setAvatar(Bitmap avatar, int index) {
        PlurkListItem item = plurks.get(index);
        item.setAvatar(avatar);
        notifyDataSetChanged();
    }
    
    public void clear() {
        this.plurks.clear();
        notifyDataSetChanged();
    }
    
    private class FetchAvatarTask extends AsyncTask<Object, Integer, Bitmap> {

        ImageView imageView;
    
        @Override
        protected Bitmap doInBackground(Object... params) {
            imageView = (ImageView) params[0];
    
            PlurkHelper p = new PlurkHelper(context);
            Bitmap avatar = p.getAvatar((String) params[1], "medium", (String) params[2]);
    
            return avatar;
        }   
    
        @Override
        protected void onPostExecute(Bitmap avatar) {
            if (avatar != null) {
                imageView.setImageBitmap(avatar);
            }   
        }   
    }
}
