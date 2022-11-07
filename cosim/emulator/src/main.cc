#include <args.hxx>
#include <glog/logging.h>

#include "vbridge.h"
#include "exceptions.h"
#include "glog_exception_safe.h"

int main(int argc, char **argv) {


  try {
    VBridge vb;
    vb.configure_simulator(argc, argv);
    vb.loop();
  } catch (TimeoutException &e) {
    return 0;
  } catch (google::CheckFailedException &e) {
    return 1;
  }
}
