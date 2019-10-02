package com.example.bogoods.page

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.bogoods.R
import com.example.bogoods.data.Pref
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.detail_order.*
import kotlinx.android.synthetic.main.profile.view.*
import java.io.IOException
import java.util.*

class DetailOrder : AppCompatActivity() {

    lateinit var dbRef: DatabaseReference
    lateinit var pref: Pref
    lateinit var fAuth: FirebaseAuth
    lateinit var imgup: ImageView
    lateinit var bt_up: TextView
    lateinit var bt_bayar: Button

    val REQUEST_CODE_IMAGE = 10002
    val PERMISSION_RC = 10003
    var value = 0.0
    lateinit var filePathImage: Uri
    lateinit var firebaseStorage: FirebaseStorage
    lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_order)
        setSupportActionBar(toolbar_detail_order)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val idorder = intent.getStringExtra("idorder")
        val orderRef = FirebaseDatabase.getInstance().getReference("order/$idorder")
        val storeRef = FirebaseDatabase.getInstance().getReference("store")
        val userRef = FirebaseDatabase.getInstance().getReference("user")
        pref = Pref(this)
        fAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        storageReference = firebaseStorage.reference

        orderRef.child("statuspembayaran").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                @SuppressLint("SetTextI18n")
                override fun onDataChange(p0: DataSnapshot) {
                    val statusbayar = p0.value.toString()
                    orderRef.child("statusbarang")
                        .addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onCancelled(p1: DatabaseError) {

                                }

                                override fun onDataChange(p1: DataSnapshot) {
                                    val statusbarang = p1.value.toString()
                                    if (statusbayar == "n") {
                                        bayar.visibility = View.VISIBLE
                                        bayar.setOnClickListener {
                                            var dialog: android.app.AlertDialog
                                            val alertDialog =
                                                android.app.AlertDialog.Builder(this@DetailOrder)
                                            val view = LayoutInflater.from(this@DetailOrder)
                                                .inflate(R.layout.popup_bayar_order, null)
                                            alertDialog.setView(view)
                                            alertDialog.setTitle("Bayar")
                                            dialog = alertDialog.create()
                                            imgup = view.findViewById(R.id.image_bukti_tf_order)
                                            bt_up = view.findViewById(R.id.bt_upload_bukti)
                                            bt_bayar = view.findViewById(R.id.bt_bayar)
                                            bt_up.setOnClickListener {
                                                when {
                                                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) -> {
                                                        if (ContextCompat.checkSelfPermission(
                                                                applicationContext,
                                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                                            )
                                                            != PackageManager.PERMISSION_GRANTED
                                                        ) {
                                                            requestPermissions(
                                                                arrayOf(
                                                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                                                ), PERMISSION_RC
                                                            )
                                                        } else {
                                                            imageChooser()
                                                        }
                                                    }
                                                    else -> {
                                                        imageChooser()
                                                    }
                                                }
                                            }
                                            bt_bayar.setOnClickListener {
                                                try {
                                                    val storageRef: StorageReference =
                                                        storageReference
                                                            .child(
                                                                "${fAuth.currentUser?.uid}/buktibayar/${pref.getUIDD()}.${GetFileExtension(
                                                                    filePathImage
                                                                )}"
                                                            )
                                                    storageRef.putFile(filePathImage)
                                                        .addOnSuccessListener {
                                                            storageRef.downloadUrl.addOnSuccessListener {
                                                                orderRef.child("buktitforder")
                                                                    .setValue(it.toString())
                                                                orderRef.child("statuspembayaran")
                                                                    .setValue("p")
                                                            }
                                                        }.addOnFailureListener {
                                                            Log.e("TAG_ERROR", it.message)
                                                        }.addOnProgressListener { taskSnapshot ->
                                                            value = (100.0 * taskSnapshot
                                                                .bytesTransferred / taskSnapshot.totalByteCount)
                                                        }
                                                } catch (e: UninitializedPropertyAccessException) {
                                                    Toast.makeText(
                                                        this@DetailOrder,
                                                        "Sukses",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                Toast.makeText(
                                                    this@DetailOrder,
                                                    "Sukses Upload Bukti \n Tunggu Konfirmasi Pembayaran dari Admin",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                dialog.dismiss()
                                            }
                                            dialog.show()
                                        }
                                        bt_beli_lagi.visibility = View.GONE
                                        status_barang_detail.text = "Menunggu Pembayaran"
                                        status_bayar.text = "Menunggu Pembayaran"
                                    } else if (statusbayar == "p") {
                                        bayar.visibility = View.GONE
                                        status_bayar.text =
                                            "Menunggu Konfirmasi Pembayaran dari Admin"
                                        status_barang_detail.text = "Menunggu Konfirmasi Pembayaran"
                                    } else if (statusbayar == "konfirmasiseller") {
                                        bayar.visibility = View.GONE
                                        status_bayar.text =
                                            "Pembayaran Lunas"
                                        status_barang_detail.text = "Menunggu Konfirmasi Pesanan dari Penjual"
                                    } else if (statusbayar == "y") {
                                        bayar.visibility = View.GONE
                                        status_bayar.text = "Pembayaran Lunas"
                                        when (statusbarang) {
                                            "p" -> {
                                                status_barang_detail.text =
                                                    "Menunggu Konfirmasi Pengiriman"
                                            }
                                            "n" -> {
                                                status_barang_detail.text = "Pesanan Dibatalkan"
                                            }
                                            "y" -> {
                                                status_barang_detail.text = "Pesanan Sedang Dikirim"
                                            }
                                            "s" -> {
                                                status_barang_detail.text = "Pesanan Sudah Diterima"
                                                bt_beli_lagi.visibility = View.VISIBLE
                                            }
                                        }
                                    }
                                }

                            }
                        )
                }

            }
        )
        orderRef.child("tglpesan").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    val tgl = p0.value.toString()
                    tanggal_pesan_detail.text = tgl
                }

            }
        )
        orderRef.child("jumlah").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                @SuppressLint("SetTextI18n")
                override fun onDataChange(p0: DataSnapshot) {
                    val jumlah = p0.value.toString()
                    jumlah_barang_detail.text = "$jumlah pcs"
                }

            }
        )
        orderRef.child("total").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    val total = p0.value.toString()
                    total_detail.text = total
                }

            }
        )
        orderRef.child("totalbayar").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    val total = p0.value.toString()
                    total_bayar.text = total
                }

            }
        )
        orderRef.child("pembayaran").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    val cok = p0.value.toString()
                    jenis_pembayaran.text = cok
                }

            }
        )
        orderRef.child("idstore").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    val idstore = p0.value.toString()
                    storeRef.child("$idstore/storename").addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {

                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                val namastore = p0.value.toString()
                                namatoko_detail.text = namastore
                            }

                        }
                    )
                    storeRef.child("$idstore/address").addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {

                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                val add = p0.value.toString()
                                alamattoko_detail.text = add
                            }

                        }
                    )
                    orderRef.child("idbarang").addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {

                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                val idbarang = p0.value.toString()
                                storeRef.child("$idstore/listbarang/$idbarang/imagebarang")
                                    .addListenerForSingleValueEvent(
                                        object : ValueEventListener {
                                            override fun onCancelled(p0: DatabaseError) {

                                            }

                                            override fun onDataChange(p0: DataSnapshot) {
                                                val image = p0.value.toString()
                                                Glide.with(this@DetailOrder)
                                                    .load(image).centerCrop()
                                                    .into(image_barang_detail)
                                            }

                                        }
                                    )
                                storeRef.child("$idstore/listbarang/$idbarang/namabarang")
                                    .addListenerForSingleValueEvent(
                                        object : ValueEventListener {
                                            override fun onCancelled(p0: DatabaseError) {

                                            }

                                            override fun onDataChange(p0: DataSnapshot) {
                                                val nama = p0.value.toString()
                                                nama_barang_detail.text = nama
                                            }

                                        }
                                    )
                                storeRef.child("$idstore/listbarang/$idbarang/harga")
                                    .addListenerForSingleValueEvent(
                                        object : ValueEventListener {
                                            override fun onCancelled(p0: DatabaseError) {

                                            }

                                            @SuppressLint("SetTextI18n")
                                            override fun onDataChange(p0: DataSnapshot) {
                                                val harga = p0.value.toString()
                                                harga_per_item_detail.text = "Rp. $harga"
                                            }

                                        }
                                    )
                            }

                        }
                    )
                }

            }
        )
        orderRef.child("idpemilikstore").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    val idpemilik = p0.value.toString()
                    userRef.child("$idpemilik/name").addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {

                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                val nama = p0.value.toString()
                                userRef.child("$idpemilik/phone").addListenerForSingleValueEvent(
                                    object : ValueEventListener {
                                        override fun onCancelled(p1: DatabaseError) {

                                        }

                                        @SuppressLint("SetTextI18n")
                                        override fun onDataChange(p1: DataSnapshot) {
                                            val phone = p1.value.toString()
                                            pemilik_toko_detail.text = "$nama ( $phone ) "
                                        }
                                    }
                                )
                            }
                        }
                    )
                }

            }
        )
        orderRef.child("alamatpengiriman")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        val alamat = p0.value.toString()
                        orderRef.child("idpembeli").addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onCancelled(p1: DatabaseError) {

                                }

                                override fun onDataChange(p1: DataSnapshot) {
                                    val idpembeli = p1.value.toString()
                                    userRef.child("$idpembeli/name").addListenerForSingleValueEvent(
                                        object : ValueEventListener {
                                            override fun onCancelled(p2: DatabaseError) {

                                            }

                                            override fun onDataChange(p2: DataSnapshot) {
                                                val name = p2.value.toString()
                                                userRef.child("$idpembeli/phone")
                                                    .addListenerForSingleValueEvent(
                                                        object : ValueEventListener {
                                                            override fun onCancelled(p3: DatabaseError) {

                                                            }

                                                            @SuppressLint("SetTextI18n")
                                                            override fun onDataChange(p3: DataSnapshot) {
                                                                val phone = p3.value.toString()
                                                                alamat_pengiriman_detail.text =
                                                                    "$name \n ( $phone ) \n $alamat"
                                                            }

                                                        }
                                                    )
                                            }

                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            )
        orderRef.child("pengiriman")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    @SuppressLint("SetTextI18n")
                    override fun onDataChange(p0: DataSnapshot) {
                        val kirim = p0.value.toString()
                        when (kirim) {
                            "menunggu" -> jenis_pengiriman.text = "Menunggu Konfirmasi Penjual"
                            "paket" -> jenis_pengiriman.text = "Jasa Paket"
                            "sendiri" -> jenis_pengiriman.text = "Diantar Penjual"
                        }
                    }
                }
            )
    }

    private fun imageChooser() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(
            Intent.createChooser(intent, "Select Image"),
            REQUEST_CODE_IMAGE
        )
    }

    fun GetFileExtension(uri: Uri): String? {
        val contentResolver = this.contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_CODE_IMAGE -> {
                filePathImage = data?.data!!
                try {
                    val bitmap: Bitmap = MediaStore
                        .Images.Media.getBitmap(
                        this.contentResolver, filePathImage
                    )
                    Glide.with(this).load(bitmap)
                        .centerCrop().into(imgup)
                } catch (x: IOException) {
                    x.printStackTrace()
                }
            }
        }
    }
}
