package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName
import java.util.Date

class Adotam {
    /// <summary>
    /// identificação da adoção
    /// </summary>
    @SerializedName("id")
    val id: Int = 0

    /// <summary>
    /// data da adoção definitiva
    /// </summary>
    @SerializedName("dateA")
    val dateA: Date = Date()

    /// <summary>
    /// FK para referenciar o utilizador que adota definitivamente um animal
    /// </summary>
    @SerializedName("utilizadorFK")
    val utilizadorFK: Int = 0

    /// <summary>
    /// Referência para o utilizador que adotou o animal
    /// </summary>
    @SerializedName("utilizador")
    val utilizador: Utilizador? = null

    /// <summary>
    /// FK para referenciar o animal que foi adotado
    /// </summary>
    @SerializedName("animalFK")
    val animalFK: Int = 0

    /// <summary>
    /// Referência para o animal adotado
    /// </summary>
    @SerializedName("animal")
    val animal: Animal? = null
}