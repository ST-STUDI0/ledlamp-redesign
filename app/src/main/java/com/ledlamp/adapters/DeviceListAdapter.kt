package com.ledlamp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ledlamp.R
import com.ledlamp.models.BleDevice

class DeviceListAdapter(
    private val devices: List<BleDevice>,
    private val onDeviceClick: (BleDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val deviceAddress: TextView = view.findViewById(R.id.deviceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.name
        holder.deviceAddress.text = device.address
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }

    override fun getItemCount(): Int = devices.size
}
