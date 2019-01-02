namespace string_finder
{
    partial class Form1
    {
        /// <summary>
        /// Variável de designer necessária.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Limpar os recursos que estão sendo usados.
        /// </summary>
        /// <param name="disposing">true se for necessário descartar os recursos gerenciados; caso contrário, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Código gerado pelo Windows Form Designer

        /// <summary>
        /// Método necessário para suporte ao Designer - não modifique 
        /// o conteúdo deste método com o editor de código.
        /// </summary>
        private void InitializeComponent()
        {
            this.choose_folder_button = new System.Windows.Forms.Button();
            this.search_button = new System.Windows.Forms.Button();
            this.folderBrowserDialog1 = new System.Windows.Forms.FolderBrowserDialog();
            this.directory_box = new System.Windows.Forms.TextBox();
            this.cancel_button = new System.Windows.Forms.Button();
            this.result_list = new System.Windows.Forms.ListView();
            this.string_box = new System.Windows.Forms.TextBox();
            this.label1 = new System.Windows.Forms.Label();
            this.SuspendLayout();
            // 
            // choose_folder_button
            // 
            this.choose_folder_button.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
            this.choose_folder_button.AutoSize = true;
            this.choose_folder_button.Location = new System.Drawing.Point(852, 10);
            this.choose_folder_button.Name = "choose_folder_button";
            this.choose_folder_button.Size = new System.Drawing.Size(125, 27);
            this.choose_folder_button.TabIndex = 0;
            this.choose_folder_button.Text = "choose folder";
            this.choose_folder_button.UseVisualStyleBackColor = true;
            // 
            // search_button
            // 
            this.search_button.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
            this.search_button.AutoSize = true;
            this.search_button.Location = new System.Drawing.Point(852, 44);
            this.search_button.Name = "search_button";
            this.search_button.Size = new System.Drawing.Size(75, 35);
            this.search_button.TabIndex = 1;
            this.search_button.Text = "Search";
            this.search_button.UseVisualStyleBackColor = true;
            // 
            // directory_box
            // 
            this.directory_box.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.directory_box.Location = new System.Drawing.Point(12, 12);
            this.directory_box.Name = "directory_box";
            this.directory_box.Size = new System.Drawing.Size(834, 22);
            this.directory_box.TabIndex = 2;
            // 
            // cancel_button
            // 
            this.cancel_button.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
            this.cancel_button.AutoSize = true;
            this.cancel_button.Enabled = false;
            this.cancel_button.Location = new System.Drawing.Point(933, 44);
            this.cancel_button.Name = "cancel_button";
            this.cancel_button.Size = new System.Drawing.Size(75, 35);
            this.cancel_button.TabIndex = 3;
            this.cancel_button.Text = "Cancel";
            this.cancel_button.UseVisualStyleBackColor = true;
            // 
            // result_list
            // 
            this.result_list.AllowColumnReorder = true;
            this.result_list.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.result_list.FullRowSelect = true;
            this.result_list.GridLines = true;
            this.result_list.Location = new System.Drawing.Point(12, 85);
            this.result_list.Name = "result_list";
            this.result_list.Size = new System.Drawing.Size(996, 462);
            this.result_list.TabIndex = 4;
            this.result_list.UseCompatibleStateImageBehavior = false;
            this.result_list.View = System.Windows.Forms.View.Details;
            // 
            // string_box
            // 
            this.string_box.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.string_box.Location = new System.Drawing.Point(117, 50);
            this.string_box.Name = "string_box";
            this.string_box.Size = new System.Drawing.Size(729, 22);
            this.string_box.TabIndex = 5;
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(9, 50);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(102, 17);
            this.label1.TabIndex = 6;
            this.label1.Text = "Text to search:";
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(8F, 16F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(1033, 559);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.string_box);
            this.Controls.Add(this.result_list);
            this.Controls.Add(this.cancel_button);
            this.Controls.Add(this.directory_box);
            this.Controls.Add(this.search_button);
            this.Controls.Add(this.choose_folder_button);
            this.MinimumSize = new System.Drawing.Size(600, 300);
            this.Name = "Form1";
            this.Text = "Text Finder";
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Button choose_folder_button;
        private System.Windows.Forms.Button search_button;
        private System.Windows.Forms.FolderBrowserDialog folderBrowserDialog1;
        private System.Windows.Forms.TextBox directory_box;
        private System.Windows.Forms.Button cancel_button;
        private System.Windows.Forms.ListView result_list;
        private System.Windows.Forms.TextBox string_box;
        private System.Windows.Forms.Label label1;
    }
}

