#import "WhisperBridge.h"
#import <AVFoundation/AVFoundation.h>
#import <vector>
#import <whisper/whisper.h>

@implementation WhisperBridge {
  struct whisper_context *_ctx;
}

- (instancetype)initWithModelPath:(NSString *)modelPath {
  self = [super init];
  if (self) {
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false; // Disable Metal on Simulator to prevent crashes
    _ctx = whisper_init_from_file_with_params([modelPath UTF8String], cparams);

    if (_ctx == NULL) {
      NSLog(@"Failed to initialize whisper context");
      return nil;
    }
  }
  return self;
}

- (void)dealloc {
  if (_ctx) {
    whisper_free(_ctx);
  }
}

- (nullable NSString *)transcribeAudioAtPath:(NSString *)audioPath {
  if (!_ctx)
    return nil;

  // Read WAV file and convert to float array
  // Note: Assuming 16kHz mono WAV for simplicity as per requirements
  // For a robust implementation, we should use AVAudioFile to read and resample
  // if needed.

  NSURL *url = [NSURL fileURLWithPath:audioPath];
  AVAudioFile *file = [[AVAudioFile alloc] initForReading:url error:nil];
  if (!file) {
    NSLog(@"Failed to open audio file");
    return nil;
  }

  AVAudioFormat *format = file.processingFormat;
  AVAudioFrameCount frameCount = (AVAudioFrameCount)file.length;
  AVAudioPCMBuffer *buffer =
      [[AVAudioPCMBuffer alloc] initWithPCMFormat:format
                                    frameCapacity:frameCount];

  [file readIntoBuffer:buffer error:nil];

  // Convert to float array (whisper expects 16kHz float)
  // We assume the input is already 16kHz mono as per Prompt 3 requirements
  // If not, resampling would be needed here.

  float *channelData = buffer.floatChannelData[0];
  NSInteger dataLength = buffer.frameLength;
  std::vector<float> pcmf32(channelData, channelData + dataLength);

  whisper_full_params wparams =
      whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
  wparams.print_special = false;
  wparams.print_progress = false;
  wparams.print_realtime = false;
  wparams.print_timestamps = false;
  wparams.translate = false;
  wparams.language = "en"; // Default to english, or passed parameter
  wparams.n_threads = 4;

  if (whisper_full(_ctx, wparams, pcmf32.data(), pcmf32.size()) != 0) {
    NSLog(@"Failed to run whisper");
    return nil;
  }

  int n_segments = whisper_full_n_segments(_ctx);
  NSMutableString *result = [NSMutableString string];

  for (int i = 0; i < n_segments; i++) {
    const char *text = whisper_full_get_segment_text(_ctx, i);
    [result appendString:[NSString stringWithUTF8String:text]];
    [result appendString:@" "]; // Add space between segments
  }

  return [result
      stringByTrimmingCharactersInSet:[NSCharacterSet
                                          whitespaceAndNewlineCharacterSet]];
}

@end
