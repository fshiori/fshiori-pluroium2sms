package org.pluroid.pluroium;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.pluroid.pluroium.data.PlurkListItem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.util.Log;
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
	
	private static HashMap<String, Integer> qualifierColorMap;

	static {
        qualifierColorMap = new HashMap<String, Integer>();
        qualifierColorMap.put("loves", Color.rgb(0xb2, 0x0c, 0x0c));
        qualifierColorMap.put("likes", Color.rgb(0xcb, 0x27, 0x28));
        qualifierColorMap.put("shares", Color.rgb(0xa7, 0x49, 0x49));
        qualifierColorMap.put("gives", Color.rgb(0x62, 0x0e, 0x0e));
        qualifierColorMap.put("hates", Color.rgb(0x00, 0x00, 0x00));
        qualifierColorMap.put("wants", Color.rgb(0x8d, 0xb2, 0x4e));
        qualifierColorMap.put("wishes", Color.rgb(0x5b, 0xb0, 0x17));
        qualifierColorMap.put("needs", Color.rgb(0x7a, 0x9a, 0x37));
        qualifierColorMap.put("will", Color.rgb(0xb4, 0x6d, 0xb9));
        qualifierColorMap.put("hopes", Color.rgb(0xe0, 0x5b, 0xe9));
        qualifierColorMap.put("asks", Color.rgb(0x83, 0x61, 0xbc));
        qualifierColorMap.put("has", Color.rgb(0x77, 0x77, 0x77));
        qualifierColorMap.put("was", Color.rgb(0x52, 0x52, 0x52));
        qualifierColorMap.put("wonders", Color.rgb(0x2e, 0x4e, 0x9e));
        qualifierColorMap.put("feels", Color.rgb(0x2d, 0x83, 0xbe));
        qualifierColorMap.put("thinks", Color.rgb(0x68, 0x9c, 0xc1));
        qualifierColorMap.put("says", Color.rgb(0xe2, 0x56, 0x0b));
        qualifierColorMap.put("is", Color.rgb(0xe5, 0x7c, 0x43));        
    }
	
	private static class ViewHolder {
        ImageView avatar;
        ImageView lock;
        TextView nickname;
        TextView qualifier;
        TextView content;
        TextView responses;
        TextView posted;
    }
	
	public PlurkListAdapter(Context context) {
		inflater = LayoutInflater.from(context);	
		plurks = new Vector<PlurkListItem>();		
	}
	
	public int getCount() {
		return plurks.size();
	}

	public PlurkListItem getItem(int index) {
		return plurks.get(index);
	}

	public long getItemId(int item) {
		return plurks.get(item).getPlurkId();
	}

	public View getView(int index, View convertView, ViewGroup parent) {
		final int ind = index;
		ViewHolder holder;
		
		Log.v(TAG, "getView");
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.plurk_list_item, null);
			holder = new ViewHolder();
			
			holder.avatar = (ImageView) convertView.findViewById(R.id.plurk_item_avatar);
			holder.lock = (ImageView) convertView.findViewById(R.id.plurk_item_lock);
			holder.nickname = (TextView) convertView.findViewById(R.id.plurk_item_owner);
			holder.qualifier = (TextView) convertView.findViewById(R.id.plurk_item_qualifier);
			holder.content = (TextView) convertView.findViewById(R.id.plurk_item_content);
			holder.posted = (TextView) convertView.findViewById(R.id.plurk_item_posted);
			holder.responses = (TextView) convertView.findViewById(R.id.plurk_item_responses);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		final PlurkListItem item = plurks.get(index);
		
		Bitmap avatar = item.getAvatar();
		if (avatar != null) {
			holder.avatar.setImageBitmap(avatar);
		} else {
			holder.avatar.setImageResource(R.drawable.avatar_unknown);
		}
		holder.nickname.setText(item.getNickname());
        holder.qualifier.setText(item.getQualifierTranslated());
        String qualifier = item.getQualifier();
        holder.qualifier.setTextColor(Color.WHITE);
        if (qualifier.equals(":")) {
        	holder.qualifier.setPadding(0, 0, 0, 0);
        	holder.qualifier.setTextColor(Color.BLACK);
            holder.qualifier.setBackgroundColor(Color.TRANSPARENT);
        } else {
        	holder.qualifier.setPadding(5, 0, 5, 0);
            holder.qualifier.setBackgroundColor(qualifierColorMap.get(qualifier));
        }
        holder.content.setText(item.getContent(), TextView.BufferType.SPANNABLE);
        holder.content.setMovementMethod(LinkMovementMethod.getInstance());
        holder.responses.setText(String.valueOf(item.getResponses()));
        
        if (item.getHasSeen() == 0) {
            holder.responses.setTextColor(Color.WHITE);
            holder.responses.setBackgroundColor(Color.rgb(0xfb, 0x00, 0x47));
        } else {
            holder.responses.setTextColor(Color.GRAY);
            holder.responses.setBackgroundColor(Color.TRANSPARENT);
        }
        
        final ListView parentList = (ListView) parent;
        convertView.setClickable(true);
        convertView.setFocusable(true);
        convertView.setBackgroundResource(R.drawable.menuitem_background);
        convertView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                parentList.performItemClick(parentList, ind, item.getPlurkId());
            	//view.performLongClick();
            }
        });
        
        holder.posted.setText(item.getPosted());
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
	
}
