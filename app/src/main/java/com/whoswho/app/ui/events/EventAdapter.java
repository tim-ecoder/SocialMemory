package com.whoswho.app.ui.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.whoswho.app.R;
import com.whoswho.app.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends BaseAdapter {

    private final Context mContext;
    private final List<Event> mEvents;
    private final SimpleDateFormat mDateFormat;

    public EventAdapter(Context context) {
        mContext = context;
        mEvents = new ArrayList<>();
        mDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    }

    public void setEvents(List<Event> events) {
        mEvents.clear();
        if (events != null) {
            mEvents.addAll(events);
        }
        notifyDataSetChanged();
    }

    public List<Event> getEvents() {
        return mEvents;
    }

    @Override
    public int getCount() {
        return mEvents.size();
    }

    @Override
    public Event getItem(int position) {
        return mEvents.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mEvents.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_event, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Event event = mEvents.get(position);
        holder.tvTitle.setText(event.getTitle());

        // Format date
        if (event.getDate() > 0) {
            holder.tvDate.setText(mDateFormat.format(new Date(event.getDate())));
        } else {
            holder.tvDate.setText("");
        }

        // Person count
        int count = event.getPersonCount();
        holder.tvPersonCount.setText(
                String.format(mContext.getString(R.string.people_count), count));

        return convertView;
    }

    static final class ViewHolder {
        final TextView tvTitle;
        final TextView tvDate;
        final TextView tvPersonCount;

        ViewHolder(View v) {
            tvTitle = (TextView) v.findViewById(R.id.tv_event_title);
            tvDate = (TextView) v.findViewById(R.id.tv_event_date);
            tvPersonCount = (TextView) v.findViewById(R.id.tv_person_count);
        }
    }
}
