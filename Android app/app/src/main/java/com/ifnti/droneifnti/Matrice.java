package com.ifnti.droneifnti;

public class Matrice {

    private float[][] data = null;
    private int rows = 0, cols = 0;

    //Android utilise majoritairement une représentation des matrices 3x3 sous forme d'un tableau à 9 cases.
    //Cette méthode permet de convertir ces tableaux en veritables objets matrice.
    public static Matrice matriceCarre(float[] donnees, int taille){
        Matrice mat = new Matrice(taille, taille);
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                mat.data[i][j] = donnees[3*i + j];
            }
        }
        return mat;
    }

    public Matrice(int rows, int cols) {
        data = new float[rows][cols];
        this.rows = rows;
        this.cols = cols;
    }

    public Matrice(float[][] data) {
        this.data = data.clone();
        rows = this.data.length;
        cols = this.data[0].length;
    }

    public float[] toArray(){
        float[] mat = new float[this.rows * this.cols];
        for(int i = 0; i < this.rows; i++){
            for(int j = 0; j < this.cols; j++){
                mat[i * this.cols + j] = this.get(i,j);
            }
        }
        return mat;
    }

    public Matrice multiplierPar(Matrice matB){
        float[][] res = new float[this.rows][matB.cols];
        for(int i = 0; i < this.rows; i++){
            for(int j = 0; j < matB.cols; j++){
                float resultat = 0.0f;
                for(int k = 0; k < this.cols; k++){
                    resultat += (this.data[i][k] * matB.get(k, j));
                }
                res[i][j] = resultat;
            }
        }
        return new Matrice(res);
    }

    @Override
    public String toString(){
        String aAfficher = "┌";
        for(int j = 0; j < this.data[0].length - 1; j++){
            aAfficher += "───────┬";
        }
        aAfficher += "───────┐\n│";

        for(int i = 0; i < this.data.length-1; i++){
            for(int j = 0; j < this.data[0].length; j++){
                aAfficher += String.format("%7.3f│", this.data[i][j]);
            }
            aAfficher += "\n├";
            for(int j = 0; j < this.data[0].length - 1; j++){
                aAfficher += "───────┼";
            }
            aAfficher += "───────┤\n│";
        }

        for(int j = 0; j < this.data[0].length; j++){
            aAfficher += String.format("%7.3f│", this.data[data.length-1][j]);
        }

        aAfficher += "\n└";
        for(int j = 0; j < this.data[0].length - 1; j++){
            aAfficher += "───────┴";
        }
        aAfficher += "───────┘";
        return aAfficher;
    }

    public boolean isSquare() {
        return rows == cols;
    }

    public Matrice transpose() {
        Matrice result = new Matrice(cols, rows);

        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                result.data[col][row] = data[row][col];
            }
        }

        return result;
    }

    // Note: exclude_row and exclude_col starts from 1
    public static Matrice subMatrice(Matrice matrix, int exclude_row, int exclude_col) {
        Matrice result = new Matrice(matrix.rows - 1, matrix.cols - 1);

        for (int row = 0, p = 0; row < matrix.rows; ++row) {
            if (row != exclude_row - 1) {
                for (int col = 0, q = 0; col < matrix.cols; ++col) {
                    if (col != exclude_col - 1) {
                        result.data[p][q] = matrix.data[row][col];

                        ++q;
                    }
                }

                ++p;
            }
        }

        return result;
    }

    public float determinant() {
        if (rows != cols) {
            return Float.NaN;
        }
        else {
            return _determinant(this);
        }
    }

    private float _determinant(Matrice matrix) {
        if (matrix.cols == 1) {
            return matrix.data[0][0];
        }
        else if (matrix.cols == 2) {
            return (matrix.data[0][0] * matrix.data[1][1] -
                    matrix.data[0][1] * matrix.data[1][0]);
        }
        else {
            float result = 0.0f;

            for (int col = 0; col < matrix.cols; ++col) {
                Matrice sub = subMatrice(matrix, 1, col + 1);

                result += (Math.pow(-1, 1 + col + 1) *
                        matrix.data[0][col] * _determinant(sub));
            }

            return result;
        }
    }

    public Matrice inverse() {
        float det = determinant();

        if (rows != cols || det == 0.0f) {
            return null;
        }
        else {
            Matrice result = new Matrice(rows, cols);

            for (int row = 0; row < rows; ++row) {
                for (int col = 0; col < cols; ++col) {
                    Matrice sub = subMatrice(this, row + 1, col + 1);

                    result.data[col][row] = (1.0f / det / (2.0f * cols) * _determinant(sub));
                }
            }

            return result;
        }
    }

    public float get(int i, int j){
        return this.data[i][j];
    }

    public float getMaxValue(){
        float max = data[0][0];
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                if(max < data[col][row]) max = data[col][row];
            }
        }
        return max;
    }

    public float getMinValue(){
        float min = data[0][0];
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                if(min > data[col][row]) min = data[col][row];
            }
        }
        return min;
    }

    public void set(int row, int col, float value){
        this.data[row][col] = value;
    }

    public static Matrice identite(int dimension){
        Matrice m = new Matrice(dimension, dimension);
        for(int i = 0; i < dimension; i++){
            m.set(i, i, 1.0f);
        }
        return m;
    }

    /**
     * Si la matrice est un vecteur (une seule colomne), renvoie sa norme
     * @return la norme du vecteur.
     */
    public float norme(){
        if(this.cols != 1){
            return Float.NaN;
        }else{
            float res = 0;
            for(int i = 0; i < this.rows; i++){
                res += this.data[i][0] * this.data[i][0];
            }
            return (float) Math.sqrt(res);
        }

    }
}