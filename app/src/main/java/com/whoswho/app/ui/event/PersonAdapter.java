package com.whoswho.app.ui.event;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.whoswho.app.R;
import com.whoswho.app.model.Person;
import com.whoswho.app.util.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class PersonAdapter extends BaseAdapter {

    private static final int AVATAR_SIZE = 96; // px, loaded at 96 and displayed at 48dp

    private final Context mContext;
    private final List<Person> mPersons;

    public PersonAdapter(Context context) {
        mContext = context;
        mPersons = new ArrayList<>();
    }

    public void setPersons(List<Person> persons) {
        mPersons.clear();
        if (persons != null) {
            mPersons.addAll(persons);
        }
        notifyDataSetChanged();
    }

    public List<Person> getPersons() {
        return mPersons;
    }

    @Override
    public int getCount() {
        return mPersons.size();
    }

    @Override
    public Person getItem(int position) {
        return mPersons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mPersons.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_person, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Person person = mPersons.get(position);

        holder.tvName.setText(person.getFullName());

        // Company / position sub-line
        String sub = buildSubLine(person);
        if (sub.isEmpty()) {
            holder.tvCompany.setVisibility(View.GONE);
        } else {
            holder.tvCompany.setVisibility(View.VISIBLE);
            holder.tvCompany.setText(sub);
        }

        // Avatar
        String photoPath = person.getPhotoPath();
        if (photoPath != null && !photoPath.isEmpty()) {
            Bitmap raw = ImageUtils.loadScaled(photoPath, AVATAR_SIZE);
            if (raw != null) {
                Bitmap circular = ImageUtils.getCircularBitmap(raw);
                holder.ivAvatar.setImageBitmap(circular);
            } else {
                holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }

    private String buildSubLine(Person p) {
        StringBuilder sb = new StringBuilder();
        if (p.getPosition() != null && !p.getPosition().isEmpty()) {
            sb.append(p.getPosition());
        }
        if (p.getCompany() != null && !p.getCompany().isEmpty()) {
            if (sb.length() > 0) sb.append(" @ ");
            sb.append(p.getCompany());
        }
        return sb.toString();
    }

    static final class ViewHolder {
        final ImageView ivAvatar;
        final TextView tvName;
        final TextView tvCompany;

        ViewHolder(View v) {
            ivAvatar = (ImageView) v.findViewById(R.id.iv_avatar);
            tvName = (TextView) v.findViewById(R.id.tv_person_name);
            tvCompany = (TextView) v.findViewById(R.id.tv_person_company);
        }
    }
}
