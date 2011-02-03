package org.pluroid.pluroium;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.pluroid.pluroium.data.PlurkListItem;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PlurkResponseAdapter extends BaseAdapter {

	private static final String TAG = "PlurkResponseAdapter";
	private LayoutInflater inflater;
	private ArrayList<PlurkListItem> responses;
	
	private Context context;
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);

	private static class ViewHolder {
		TextView displayname;
		TextView qualifier;
        TextView content;
        TextView posted;
    }
	
	public PlurkResponseAdapter(Context context) {
		this.context = context;
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
		final int ind = index;
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.plurk_response_item, null);
			holder = new ViewHolder();
			
			holder.displayname = (TextView) convertView.findViewById(R.id.plurk_response_author);
			holder.qualifier = (TextView) convertView.findViewById(R.id.plurk_response_qualifier);
			holder.content = (TextView) convertView.findViewById(R.id.plurk_response_content);
			holder.posted = (TextView) convertView.findViewById(R.id.plurk_response_posted);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		final PlurkListItem item = responses.get(index);
		
		holder.displayname.setText(item.getNickname());
		
		Resources res = context.getResources();
        String qT = item.getQualifierTranslated();
        String qualifier = item.getQualifier();
        int colorId = res.getIdentifier("qualifier_" + qualifier, "color", "org.ericsk.pluroid");
        
        if (colorId > 0) {
            holder.qualifier.setText(qT);
        	holder.qualifier.setTextColor(Color.WHITE);
        	holder.qualifier.setBackgroundColor(res.getColor(colorId));
        } else {
        	holder.qualifier.setText(":");
        	holder.qualifier.setTextColor(Color.BLACK);
        	holder.qualifier.setBackgroundColor(Color.TRANSPARENT);
        }
		
        holder.content.setText(Html.fromHtml(item.getContent(), new ImageGetter(context), null));
        holder.content.setMovementMethod(LinkMovementMethod.getInstance());
        
        holder.posted.setText(sdf.format(item.getPosted()));

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
