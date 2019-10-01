package com.example.bogoods.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.Image
import android.util.Log
import com.example.bogoods.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bogoods.data.Pref
import com.example.bogoods.model.CartModel
import com.example.bogoods.model.ListBarangModel
import com.example.bogoods.model.StoreModel
import com.example.bogoods.model.UserModel
import com.example.bogoods.page.DetailPesanan
import com.example.bogoods.page.EditStore
import com.example.bogoods.page.ListBarang
import com.example.bogoods.page.ListRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class CartAdapter : RecyclerView.Adapter<CartAdapter.ViewHolder> {
    lateinit var mCtx: Context
    lateinit var itemStore: List<CartModel>
    lateinit var pref: Pref
    lateinit var dbRef: DatabaseReference
    lateinit var fauth: FirebaseAuth
    lateinit var btplus: RelativeLayout
    lateinit var btmin: RelativeLayout
    lateinit var perbarui: Button
    lateinit var stokdin: TextView
    lateinit var tvstok: TextView
    lateinit var namabarang_popup: TextView
    lateinit var jumlah_barang_popup: TextView
    lateinit var total_popup: TextView
    lateinit var set_alamat: EditText
    lateinit var order_popup: Button
    lateinit var cartTotal : CartTotal

    constructor()
    constructor(mCtx: Context, list: List<CartModel>) {
        this.mCtx = mCtx
        this.itemStore = list
        cartTotal = mCtx as DetailPesanan
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_cart, parent, false)
        val ViewHolder = ViewHolder(view)
        return ViewHolder
    }

    override fun getItemCount(): Int {
        return itemStore.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model: CartModel = itemStore.get(position)
        fauth = FirebaseAuth.getInstance()
        FirebaseDatabase.getInstance()
            .getReference("store/")
            .child(model.idstore!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(data2: DataSnapshot) {
                    val data =
                        data2.getValue(StoreModel::class.java)
                    model.storeModel = data
                    holder.namastore.text = model.storeModel!!.storename
                }

                override fun onCancelled(holder: DatabaseError) {
                    Log.e("cok", holder.message)
                }
            })
        FirebaseDatabase.getInstance()
            .getReference("store/")
            .child(model.idstore!!).child("listbarang/${model.idbarang}")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(data2: DataSnapshot) {
                    val data =
                        data2.getValue(ListBarangModel::class.java)
                    model.barangModel = data
                    holder.namabarang.text = model.barangModel!!.namabarang
                    holder.harga_per_item.text = "Rp. " + model.barangModel!!.harga + " /item"
                    Glide.with(mCtx).load(model.barangModel!!.imagebarang)
                        .centerCrop()
                        .error(R.drawable.ic_seller)
                        .into(holder.image)
                    holder.stok.text = model.jumlah + " pcs"
                    holder.subtotal.text = "Total Rp. " + (model.jumlah.toString().toInt() * model.barangModel!!.harga.toString().toInt()).toString()
                    cartTotal.total(model.jumlah.toString().toInt() * model.barangModel!!.harga.toString().toInt())
                }

                override fun onCancelled(holder: DatabaseError) {
                    Log.e("cok", holder.message)
                }
            })
        holder.editstok.setOnClickListener {
            var dialog: android.app.AlertDialog
            val alertDialog = android.app.AlertDialog.Builder(mCtx)
            val view = LayoutInflater.from(mCtx).inflate(R.layout.popup_list_cart, null)
            alertDialog.setView(view)
            stokdin = view.findViewById(R.id.tv_stok_dinamis)
            tvstok = view.findViewById(R.id.tv_stok_popup)
            btplus = view.findViewById(R.id.bt_plus_stok)
            btmin = view.findViewById(R.id.bt_min_stok)
            perbarui = view.findViewById(R.id.perbaruistok)
            FirebaseDatabase.getInstance().getReference("cart/${model.idcart}")
                .child("jumlah").addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {

                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            stokdin.setText(p0.value.toString())
                        }

                    }
                )
            FirebaseDatabase.getInstance()
                .getReference("store/")
                .child(model.idstore!!).child("listbarang/${model.idbarang}")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("SetTextI18n")
                    override fun onDataChange(data2: DataSnapshot) {
                        val data =
                            data2.getValue(ListBarangModel::class.java)
                        model.barangModel = data
                        tvstok.text = model.barangModel!!.stok
                    }

                    override fun onCancelled(holder: DatabaseError) {
                        Log.e("cok", holder.message)
                    }
                })
            btplus.setOnClickListener {
                if (stokdin.text.toString().toInt() == tvstok.text.toString().toInt()) {
                    Toast.makeText(mCtx, "Stok Habis", Toast.LENGTH_SHORT).show()
                } else {
                    stokdin.text = (stokdin.text.toString().toInt() + 1).toString()
                }
            }
            btmin.setOnClickListener {
                if (stokdin.text.toString().toInt() == 1) {
                    Toast.makeText(mCtx, "Tambah Stok", Toast.LENGTH_SHORT).show()
                } else {
                    stokdin.text = (stokdin.text.toString().toInt() - 1).toString()
                }
            }
            dialog = alertDialog.create()
            perbarui.setOnClickListener {
                FirebaseDatabase.getInstance().getReference("cart")
                    .child("${model.idcart}").child("jumlah").setValue(stokdin.text.toString())
                dialog.dismiss()
            }
            dialog.show()
        }
        holder.hapus.setOnClickListener {
            var dialog: android.app.AlertDialog
            val alertDialog = android.app.AlertDialog.Builder(mCtx)
            alertDialog.setTitle("HAPUS CART")
            alertDialog.setPositiveButton("HAPUS"){dialog, i ->
                FirebaseDatabase.getInstance().getReference("cart").child("${model.key}").removeValue()
                Toast.makeText(mCtx, "Sukses Hapus", Toast.LENGTH_SHORT).show()
            }
            alertDialog.setNegativeButton("CANCEL"){dialog, i ->
                dialog.dismiss()
            }
            dialog = alertDialog.create()
            dialog.show()
        }
        holder.checkoutnow.setOnClickListener {
            var dialog: android.app.AlertDialog
            val alertDialog = android.app.AlertDialog.Builder(mCtx)
            val view = LayoutInflater.from(mCtx).inflate(R.layout.popup_checkout_now, null)
            alertDialog.setView(view)
            alertDialog.setTitle("CHECK OUT NOW")
            dialog = alertDialog.create()
            namabarang_popup = view.findViewById(R.id.nama_barang_popup)
            jumlah_barang_popup = view.findViewById(R.id.stok_popup)
            total_popup = view.findViewById(R.id.total_popup)
            set_alamat = view.findViewById(R.id.alamat_pengiriman)
            order_popup = view.findViewById(R.id.order_now_popup)

            FirebaseDatabase.getInstance()
                .getReference("store/")
                .child(model.idstore!!).child("listbarang/${model.idbarang}")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("SetTextI18n")
                    override fun onDataChange(data2: DataSnapshot) {
                        val data =
                            data2.getValue(ListBarangModel::class.java)
                        model.barangModel = data
                        namabarang_popup.text = model.barangModel!!.namabarang
                        val harga = model.barangModel!!.harga
                        total_popup.text = (harga.toString().toInt() * model.jumlah.toString().toInt()).toString()
                    }

                    override fun onCancelled(holder: DatabaseError) {
                        Log.e("cok", holder.message)
                    }
                })
            val alamat = set_alamat.text.toString()
            val idorder = UUID.randomUUID().toString()
            jumlah_barang_popup.text = model.jumlah
            order_popup.setOnClickListener {

                val db_add_order = FirebaseDatabase.getInstance().getReference("order/$idorder")
                db_add_order.child("jumlah").setValue(model.jumlah.toString())
                db_add_order.child("idbarang").setValue(model.idbarang.toString())
                db_add_order.child("idstore").setValue(model.idstore.toString())
                db_add_order.child("idpembeli").setValue(model.idpembeli.toString())
                db_add_order.child("idpemilikstore").setValue(model.idpemilikstore.toString())
                db_add_order.child("alamatpengiriman").setValue(alamat)
                db_add_order.child("total").setValue(total_popup.text.toString())

                FirebaseDatabase.getInstance().getReference("cart")
                    .child("${model.key}").removeValue()

                dialog.dismiss()
            }
            dialog.show()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var rl: RelativeLayout
        var namastore: TextView
        var namabarang: TextView
        var stok: TextView
        var harga_per_item: TextView
        var subtotal: TextView
        var image: ImageView
        var hapus: RelativeLayout
        var checkoutnow: RelativeLayout
        var editstok: TextView

        init {
            rl = itemView.findViewById(R.id.rel_)
            namastore = itemView.findViewById(R.id.storename_cart)
            image = itemView.findViewById(R.id.imagebarang_cart)
            namabarang = itemView.findViewById(R.id.item_name)
            harga_per_item = itemView.findViewById(R.id.harga_per_item)
            subtotal = itemView.findViewById(R.id.subtotal)
            stok = itemView.findViewById(R.id.stok_cart)
            hapus = itemView.findViewById(R.id.hapus_cart)
            checkoutnow = itemView.findViewById(R.id.co_now_cart)
            editstok = itemView.findViewById(R.id.edit_stok)
        }
    }

    interface CartTotal{
        fun total(jumlah : Int)
    }
}