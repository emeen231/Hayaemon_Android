/*
 * EqualizersAdapter
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

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class EqualizersAdapter extends RecyclerView.Adapter<EqualizersAdapter.ViewHolder>
{
    MainActivity activity;
    private int resource;
    private List<EqualizerItem> items = null;
    private LayoutInflater inflater;
    private int nPosition;

    public void setPosition(int nPosition) { this.nPosition = nPosition; }
    public int getPosition() { return nPosition; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout equalizerItem;
        TextView textEqualizer;

        ViewHolder(View view) {
            super(view);
            equalizerItem = (RelativeLayout) view.findViewById(R.id.equalizerItem);
            textEqualizer = (TextView) view.findViewById(R.id.textEqualizer);
        }
    }

    public EqualizersAdapter(Context context, int resource, List<EqualizerItem> items)
    {
        this.activity = (MainActivity)context;
        this.resource = resource;
        this.items = items;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public EqualizersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(resource, parent, false);

        return new EqualizersAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final EqualizersAdapter.ViewHolder holder, final int position)
    {
        EqualizerItem item = items.get(position);
        String name = item.getEqualizerName();
        final EqualizerFragment equalizerFragment = (EqualizerFragment)activity.mSectionsPagerAdapter.getItem(3);
        holder.textEqualizer.setText(name);

        if(equalizerFragment.isSelectedItem(position))
            holder.itemView.setBackgroundColor(Color.argb(255, 170, 170, 170));
        else
            holder.itemView.setBackgroundColor(Color.argb(255, 255, 255, 255));
        holder.equalizerItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            equalizerFragment.onEqualizerItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }
}
