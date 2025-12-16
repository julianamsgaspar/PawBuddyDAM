package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName
import java.util.Date

class Adotam (

    @SerializedName("id")
    val id: Int,

    @SerializedName("dateA")
    val dateA: String,

    @SerializedName("utilizadorFK")
    val utilizadorFK: Int,

    @SerializedName("utilizador")
    val utilizador: Utilizador?,

    @SerializedName("animalFK")
    val animalFK: Int,

    @SerializedName("animal")
    val animal: Animal?
)