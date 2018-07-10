package com.downloader.sdk.dlplugin.demo;




import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class DownloadCheckBox extends ImageView
{
  private boolean isChecked;
  private int imgNormal;//非选中状态
  private int imgChecked;//选中状态图片

  public DownloadCheckBox(Context paramContext)
  {
    super(paramContext);
    init();
  }

  public DownloadCheckBox(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
    init();
  }

  public DownloadCheckBox(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
  {
    super(paramContext, paramAttributeSet, paramInt);
    init();
  }

  private void init()
  {
    boolean bool = this.isChecked;
    doCheck(bool);
  }

  public void doCheck(boolean checkStatus)
  {
    this.isChecked = checkStatus;
    if (this.isChecked)
    {
      if (this.imgChecked != 0)
      {
        int i = this.imgChecked;
        setImageResource(i);
        return;
      }
      setImageResource(R.drawable.download_item_checkbox_selected);
      return;
    }
    if (this.imgNormal != 0)
    {
      int j = this.imgNormal;
      setImageResource(j);
      return;
    }
    setImageResource(R.drawable.download_item_checkbox_unselected);
  }

  public boolean isChecked()
  {
    return this.isChecked;
  }
}