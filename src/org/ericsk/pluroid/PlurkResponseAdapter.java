package org.ericsk.pluroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ericsk.pluroid.data.PlurkListItem;

import android.content.Context;
import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PlurkResponseAdapter extends BaseAdapter {

	private static final String TAG = "PlurkResponseAdapter";
	private LayoutInflater inflater;
	private ArrayList<PlurkListItem> responses;
	
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
        TextView nickname;
        TextView qualifier;
        TextView content;
        TextView posted;
    }
	
	
	public PlurkResponseAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		responses = new ArrayList<PlurkListItem>();
		notifyDataSetChanged();
	}
	
	public int getCount() {
		return responses.size();
	}

	public Object getItem(int index) {
		return responses.get(index);
	}

	public long getItemId(int item) {
		return responses.get(item).getPlurkId();
	}

	public View getView(int index, View convertView, ViewGroup parent) {
		ViewHolder holder;

		Log.v(TAG, "getView");
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.plurk_response_item, null);
			holder = new ViewHolder();
			
			holder.nickname = (TextView) convertView.findViewById(R.id.plurk_response_owner);
			holder.qualifier = (TextView) convertView.findViewById(R.id.plurk_response_qualifier);
			holder.content = (TextView) convertView.findViewById(R.id.plurk_response_content);
			holder.posted = (TextView) convertView.findViewById(R.id.plurk_response_posted);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		final PlurkListItem item = responses.get(index);
		
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
        
        
        holder.posted.setText(item.getPosted());

        return convertView;
	}

	public void setResponses(List<PlurkListItem> responses) {
		this.responses = (ArrayList<PlurkListItem>) responses;
		notifyDataSetChanged();
	}
	
	public void addResponses(List<PlurkListItem> responses) {
		this.responses.addAll(responses);
		notifyDataSetInvalidated();
	}
	
	public void clear() {
		this.responses.clear();
		notifyDataSetChanged();
	}
}
